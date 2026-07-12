require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { createClient } = require('@supabase/supabase-js');

const app = express();
const PORT = process.env.PORT || 10000;

// Enable CORS and JSON parsing
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

// Helper: Ensure Supabase Storage Bucket Exists
async function ensureBucketExists() {
  try {
    const { data: buckets, error: listError } = await supabase.storage.listBuckets();
    if (listError) throw listError;
    const exists = buckets.some(b => b.name === 'esports_images');
    if (!exists) {
      const { data, error } = await supabase.storage.createBucket('esports_images', {
        public: true,
        fileSizeLimit: 5242880, // 5MB
        allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp']
      });
      if (error) {
        console.error("Error creating bucket esports_images:", error);
      } else {
        console.log("Bucket 'esports_images' created successfully.");
      }
    }
  } catch (err) {
    console.error("ensureBucketExists error:", err);
  }
}

// Upload Endpoint
app.post('/api/upload', async (req, res) => {
  try {
    const { image, filename, mimeType } = req.body;
    if (!image) {
      return res.status(400).json({ error: "No image data provided" });
    }
    
    // Check if image is already a full URL
    if (image.startsWith('http://') || image.startsWith('https://')) {
      return res.json({ success: true, url: image });
    }

    await ensureBucketExists();

    const cleanFilename = `${Date.now()}_${(filename || 'image.jpg').replace(/[^a-zA-Z0-9._-]/g, '')}`;
    const buffer = Buffer.from(image, 'base64');

    const { data, error } = await supabase.storage
      .from('esports_images')
      .upload(cleanFilename, buffer, {
        contentType: mimeType || 'image/jpeg',
        upsert: true
      });

    if (error) {
      console.error("Supabase Storage upload failed:", error);
      throw error;
    }

    const { data: publicUrlData } = supabase.storage
      .from('esports_images')
      .getPublicUrl(cleanFilename);

    res.json({ success: true, url: publicUrlData.publicUrl });
  } catch (err) {
    console.error("Upload handler error:", err);
    res.status(500).json({ error: err.message });
  }
});


// Initialize Supabase Client
const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_SECRET_KEY || process.env.SUPABASE_ANON_KEY;
const hasServiceRoleKey = !!(process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_SECRET_KEY);

if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
  console.error("CRITICAL ERROR: SUPABASE_URL or SUPABASE_ANON_KEY/SUPABASE_SERVICE_ROLE_KEY is missing in environment variables!");
  process.exit(1);
}

// Service role key is used to bypass RLS policies and handle transactions securely on the backend
const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
  auth: {
    persistSession: false,
    autoRefreshToken: false
  }
});

// Helper: Generate Referral Code
function generateReferralCode() {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let code = "";
  for (let i = 0; i < 8; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
}

// Basic Liveness Check
app.get('/health', (req, res) => {
  res.status(200).json({ status: "healthy", timestamp: new Date() });
});

// ==========================================
// AUTHENTICATION & PROFILE ENDPOINTS
// ==========================================

// Sign Up Endpoint
app.post('/api/auth/signup', async (req, res) => {
  try {
    const { whatsapp, name, password, referral_code, device_id } = req.body;
    if (!whatsapp || !password) {
      return res.status(400).json({ error: "WhatsApp number and password are required" });
    }

    if (device_id) {
      const { count, error: countErr } = await supabase
        .from('transactions')
        .select('*', { count: 'exact', head: true })
        .eq('type', 'DEVICE_REGISTRATION')
        .eq('reference_number', device_id);
      
      if (!countErr && count >= 2) {
        return res.status(400).json({ error: "Maximum 2 accounts can be registered from this device." });
      }
    }

    const maskedEmail = `${whatsapp}@arenaesports.com`;

    let authData, authError;
    if (hasServiceRoleKey) {
      // Sign up user via Supabase Auth Admin API (bypassing email confirmation requirement)
      const { data, error } = await supabase.auth.admin.createUser({
        email: maskedEmail,
        password: password,
        email_confirm: true,
        user_metadata: {
          whatsapp_number: whatsapp,
          name: name || 'Gamer'
        }
      });
      authData = data;
      authError = error;
    } else {
      // Fallback to standard signUp if service role key is not available
      const { data, error } = await supabase.auth.signUp({
        email: maskedEmail,
        password: password,
        options: {
          data: {
            whatsapp_number: whatsapp,
            name: name || 'Gamer'
          }
        }
      });
      authData = data;
      authError = error;
    }

    if (authError) throw authError;

    const userId = authData.user?.id;
    if (!userId) {
      return res.status(500).json({ error: "Failed to create authentication account" });
    }

    // Sign in to get active session access token
    let accessToken = null;
    try {
      const { data: sessionData, error: sessionError } = await supabase.auth.signInWithPassword({
        email: maskedEmail,
        password: password
      });
      if (!sessionError && sessionData.session) {
        accessToken = sessionData.session.access_token;
      }
    } catch (sessErr) {
      console.warn("Failed to generate session for new user, client will log in manually:", sessErr);
    }

    // 2. Generate own referral code and create profile in profiles table
    const ownReferralCode = generateReferralCode();
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .insert([
        {
          id: userId,
          whatsapp_number: whatsapp,
          name: name || 'Gamer',
          own_referral_code: ownReferralCode,
          referral_code_used: referral_code || null,
          deposit_balance: 0.0,
          withdrawal_balance: 0.0,
          referred_count: 0
        }
      ])
      .select()
      .single();

    if (profileError) {
      // Clean up Auth user if profile insertion failed
      try {
        if (hasServiceRoleKey) {
          await supabase.auth.admin.deleteUser(userId);
        }
      } catch (delErr) {
        console.error("Failed to delete auth user after profile insert error:", delErr);
      }
      throw profileError;
    }

    // 3. Referral Reward is now given only when the referred user deposits a minimum amount.

    // 4. Log device registration
    if (device_id) {
      await supabase.from('transactions').insert([{
        whatsapp_number: whatsapp,
        type: 'DEVICE_REGISTRATION',
        amount: 0,
        upi_id: 'SYSTEM',
        reference_number: device_id,
        status: 'APPROVED',
        timestamp: Date.now()
      }]);
    }

    res.status(201).json({
      success: true,
      user: {
        id: userId,
        whatsapp_number: whatsapp,
        name: profile.name,
        own_referral_code: ownReferralCode,
        referral_code_used: referral_code || null,
        deposit_balance: 0.0,
        withdrawal_balance: 0.0,
        referred_count: 0
      },
      access_token: accessToken
    });
  } catch (error) {
    console.error("SignUp error:", error);
    res.status(500).json({ error: error.message });
  }
});

// Login Endpoint
app.post('/api/auth/login', async (req, res) => {
  try {
    const { whatsapp, password } = req.body;
    if (!whatsapp || !password) {
      return res.status(400).json({ error: "WhatsApp number and password are required" });
    }

    const maskedEmail = `${whatsapp}@arenaesports.com`;

    // Sign in via Supabase Auth
    const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
      email: maskedEmail,
      password: password
    });

    if (authError) throw authError;

    // Fetch user profile from database
    const { data: profile, error: profileError } = await supabase
      .from('profiles')
      .select('*')
      .like('whatsapp_number', `${whatsapp}%`)
      .single();

    if (profileError) throw profileError;

    res.json({
      success: true,
      user: { ...profile, is_admin: profile.is_admin || profile.whatsapp_number === '6202778501' },
      access_token: authData.session?.access_token || null
    });
  } catch (error) {
    console.error("Login error:", error);
    res.status(401).json({ error: error.message });
  }
});

// Get user profile by WhatsApp number
app.get('/api/users/:whatsapp', async (req, res) => {
  try {
    const { whatsapp } = req.params;
    const { data, error } = await supabase
      .from('profiles')
      .select('*')
      .like('whatsapp_number', `${whatsapp}%`)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return res.status(404).json({ error: "Profile not found" });
      }
      throw error;
    }

    res.json({ success: true, user: { ...data, is_admin: data.is_admin || data.whatsapp_number === '6202778501' } });
  } catch (error) {
    console.error("Error fetching profile:", error);
    res.status(500).json({ error: error.message });
  }
});


// ==========================================
// TOURNAMENT ENDPOINTS
// ==========================================

// Get all tournaments
app.get('/api/tournaments', async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('tournaments')
      .select('*')
      .order('id', { ascending: false });

    if (error) throw error;
    res.json({ success: true, tournaments: data });
  } catch (error) {
    console.error("Error fetching tournaments:", error);
    res.status(500).json({ error: error.message });
  }
});

// Get tournament details by ID
app.get('/api/tournaments/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { data, error } = await supabase
      .from('tournaments')
      .select('*')
      .eq('id', id)
      .single();

    if (error) {
      if (error.code === 'PGRST116') {
        return res.status(404).json({ error: "Tournament not found" });
      }
      throw error;
    }


    res.json({ success: true, tournament: data });
  } catch (error) {
    console.error("Error fetching tournament:", error);
    res.status(500).json({ error: error.message });
  }
});
// Create a new tournament (Admin)
app.post('/api/tournaments', async (req, res) => {
  try {
    const { game, title, poster_res, entry_fee, prize_pool, prize_1st, prize_2nd, prize_3rd, prize_4th, rules, start_time } = req.body;

    if (!game || !title || !start_time) {
      return res.status(400).json({ error: "game, title, and start_time are required fields" });
    }

    const { data, error } = await supabase
      .from('tournaments')
      .insert([
        {
          game,
          title,
          poster_res: poster_res || 'default_poster',
          entry_fee: parseFloat(entry_fee) || 0.0,
          prize_pool: parseFloat(prize_pool) || 0.0,
          prize_1st: parseFloat(prize_1st) || 0.0,
          prize_2nd: parseFloat(prize_2nd) || 0.0,
          prize_3rd: parseFloat(prize_3rd) || 0.0,
          prize_4th: parseFloat(prize_4th) || 0.0,
          rules: rules || '',
          room_id: null,
          room_password: null,
          start_time
        }
      ])
      .select()
      .single();

    if (error) throw error;
    res.status(201).json({ success: true, tournament: data });
  } catch (error) {
    console.error("Error creating tournament:", error);
    res.status(500).json({ error: error.message });
  }
});

// Update Room details for tournament (Admin)
app.patch('/api/tournaments/:id/room', async (req, res) => {
  try {
    const { id } = req.params;
    const { room_id, room_password, start_time } = req.body;

    const updateObj = {};
    if (room_id !== undefined) updateObj.room_id = room_id;
    if (room_password !== undefined) updateObj.room_password = room_password;
    if (start_time !== undefined) updateObj.start_time = start_time;

    const { data, error } = await supabase
      .from('tournaments')
      .update(updateObj)
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    res.json({ success: true, tournament: data });
  } catch (error) {
    console.error("Error updating room info:", error);
    res.status(500).json({ error: error.message });
  }
});

// Start Tournament (Admin)
app.post('/api/tournaments/:id/start', async (req, res) => {
  try {
    const { id } = req.params;

    const { data: tour, error: fetchErr } = await supabase
      .from('tournaments')
      .select('start_time')
      .eq('id', id)
      .single();

    if (fetchErr) throw fetchErr;
    if (!tour) return res.status(404).json({ error: "Tournament not found" });

    let currentStartTime = tour.start_time || "";
    if (!currentStartTime.includes("[STARTED]")) {
      currentStartTime = currentStartTime.replace("[FINISHED]", "").trim() + " [STARTED]";
    }

    const { data, error } = await supabase
      .from('tournaments')
      .update({ start_time: currentStartTime })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    res.json({ success: true, tournament: data });
  } catch (error) {
    console.error("Error starting tournament:", error);
    res.status(500).json({ error: error.message });
  }
});

// Finish Tournament (Admin)
app.post('/api/tournaments/:id/finish', async (req, res) => {
  try {
    const { id } = req.params;

    const { data: tour, error: fetchErr } = await supabase
      .from('tournaments')
      .select('start_time')
      .eq('id', id)
      .single();

    if (fetchErr) throw fetchErr;
    if (!tour) return res.status(404).json({ error: "Tournament not found" });

    let currentStartTime = tour.start_time || "";
    currentStartTime = currentStartTime.replace("[STARTED]", "").trim();
    if (!currentStartTime.includes("[FINISHED]")) {
      currentStartTime = currentStartTime + " [FINISHED]";
    }

    const { data, error } = await supabase
      .from('tournaments')
      .update({ 
        start_time: currentStartTime,
        room_id: null,
        room_password: null
      })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    res.json({ success: true, tournament: data });
  } catch (error) {
    console.error("Error finishing tournament:", error);
    res.status(500).json({ error: error.message });
  }
});

// Delete Tournament (Admin)
app.delete('/api/tournaments/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { error } = await supabase
      .from('tournaments')
      .delete()
      .eq('id', id);

    if (error) throw error;
    res.json({ success: true, message: `Tournament ${id} deleted successfully.` });
  } catch (error) {
    console.error("Error deleting tournament:", error);
    res.status(500).json({ error: error.message });
  }
});


// ==========================================
// REGISTRATION & BOOKING ENDPOINTS
// ==========================================

// Register a user for a tournament (debits balance automatically)
app.post('/api/tournaments/register', async (req, res) => {
  try {
    const { whatsapp_number, tournament_id } = req.body;

    if (!whatsapp_number || !tournament_id) {
      return res.status(400).json({ error: "whatsapp_number and tournament_id are required" });
    }

    // Support formatted whatsapp_number like raw_whatsapp|team_name|members
    let raw_whatsapp = whatsapp_number;
    if (whatsapp_number.includes('|')) {
      raw_whatsapp = whatsapp_number.split('|')[0];
    }

    // 1. Fetch user profile and verify balance
    const { data: user, error: userError } = await supabase
      .from('profiles')
      .select('*')
      .eq('whatsapp_number', raw_whatsapp)
      .single();

    if (userError || !user) {
      return res.status(404).json({ error: "User profile not found" });
    }

    // 2. Fetch tournament to verify entry fee
    const { data: tournament, error: tourneyError } = await supabase
      .from('tournaments')
      .select('*')
      .eq('id', tournament_id)
      .single();

    if (tourneyError || !tournament) {
      return res.status(404).json({ error: "Tournament not found" });
    }

    const entryFee = tournament.entry_fee || 0.0;
    const totalBalance = (user.deposit_balance || 0.0) + (user.withdrawal_balance || 0.0);

    if (totalBalance < entryFee) {
      return res.status(400).json({ error: "Insufficient balance to join tournament" });
    }

    // Check if already registered (starts with raw_whatsapp)
    const { data: allRegs, error: checkRegError } = await supabase
      .from('registrations')
      .select('id, whatsapp_number')
      .eq('tournament_id', tournament_id);

    if (allRegs) {
      const isAlreadyJoined = allRegs.some(reg => {
        const regRaw = reg.whatsapp_number.split('|')[0];
        return regRaw === raw_whatsapp;
      });
      if (isAlreadyJoined) {
        return res.status(400).json({ error: "You are already registered for this tournament" });
      }
    }

    // 3. Deduct entry fee (prioritize deposit balance first)
    let newDeposit = user.deposit_balance || 0.0;
    let newWithdrawal = user.withdrawal_balance || 0.0;

    if (newDeposit >= entryFee) {
      newDeposit -= entryFee;
    } else {
      const remaining = entryFee - newDeposit;
      newDeposit = 0.0;
      newWithdrawal -= remaining;
    }

    // Update balances
    const { error: balanceError } = await supabase
      .from('profiles')
      .update({
        deposit_balance: newDeposit,
        withdrawal_balance: newWithdrawal
      })
      .eq('id', user.id);

    if (balanceError) throw balanceError;

    // 4. Create Registration record (use the full formatted whatsapp_number)
    const { data: registration, error: regError } = await supabase
      .from('registrations')
      .insert([
        {
          whatsapp_number, // formatted user input: e.g. "6202778501|Team Name|Members|Slot"
          tournament_id,
          timestamp: Date.now()
        }
      ])
      .select()
      .single();

    if (regError) {
      // rollback balances on failure
      await supabase
        .from('profiles')
        .update({
          deposit_balance: user.deposit_balance,
          withdrawal_balance: user.withdrawal_balance
        })
        .eq('id', user.id);
      throw regError;
    }

    // Log the tournament entry in transaction history
    await supabase
      .from('transactions')
      .insert([
        {
          whatsapp_number: raw_whatsapp,
          type: 'TOURNAMENT_ENTRY',
          amount: entryFee,
          upi_id: 'SYSTEM',
          reference_number: `${tournament.title || 'Tournament'} (Reg: ${registration.id})`,
          status: 'APPROVED',
          timestamp: Date.now()
        }
      ]);

    // 5. Insert to Game History (as pending, using raw_whatsapp)
    await supabase
      .from('game_histories')
      .insert([
        {
          whatsapp_number: raw_whatsapp,
          game_name: `${tournament.game} - ${tournament.title}`,
          prize_won: null,
          status: 'PENDING',
          timestamp: Date.now()
        }
      ]);

    res.status(201).json({
      success: true,
      registration,
      updatedBalances: { deposit: newDeposit, withdrawal: newWithdrawal }
    });
  } catch (error) {
    console.error("Error registering tournament:", error);
    res.status(500).json({ error: error.message });
  }
});

// Fetch user registrations list
app.get('/api/users/:whatsapp/registrations', async (req, res) => {
  try {
    const { whatsapp } = req.params;
    const { data, error } = await supabase
      .from('registrations')
      .select('*')
      .like('whatsapp_number', `${whatsapp}%`);

    if (error) throw error;
    res.json({ success: true, registrations: data });
  } catch (error) {
    console.error("Error fetching registrations:", error);
    res.status(500).json({ error: error.message });
  }
});


// ==========================================
// WALLET & TRANSACTION ENDPOINTS
// ==========================================

// Submit a standard transaction (Deposit / Withdrawal request)
app.post('/api/transactions', async (req, res) => {
  try {
    const { whatsapp_number, type, amount, upi_id, reference_number } = req.body;

    if (!whatsapp_number || !type || !amount) {
      return res.status(400).json({ error: "whatsapp_number, type, and amount are required" });
    }

    // Check balance if type is WITHDRAWAL
    if (type === 'WITHDRAWAL') {
      const { data: user, error: userError } = await supabase
        .from('profiles')
        .select('*')
        .eq('whatsapp_number', whatsapp_number)
        .single();

      if (userError || !user) {
        return res.status(404).json({ error: "User profile not found" });
      }

      const withdrawalAmount = parseFloat(amount);
      if ((user.withdrawal_balance || 0.0) < withdrawalAmount) {
        return res.status(400).json({ error: "Insufficient withdrawal balance" });
      }

      // Deduct immediately on submission so user cannot double-spend pending withdrawals
      const newWithdrawal = (user.withdrawal_balance || 0.0) - withdrawalAmount;
      const { error: balanceError } = await supabase
        .from('profiles')
        .update({ withdrawal_balance: newWithdrawal })
        .eq('id', user.id);

      if (balanceError) throw balanceError;
    }

    const { data, error } = await supabase
      .from('transactions')
      .insert([
        {
          whatsapp_number,
          type,
          amount: parseFloat(amount),
          upi_id: upi_id || '',
          reference_number: reference_number || null,
          status: 'PENDING',
          timestamp: Date.now()
        }
      ])
      .select()
      .single();

    if (error) {
      // Rollback withdrawal deduction if transaction insertion fails
      if (type === 'WITHDRAWAL') {
        const { data: user } = await supabase
          .from('profiles')
          .select('withdrawal_balance')
          .eq('whatsapp_number', whatsapp_number)
          .single();
        if (user) {
          await supabase
            .from('profiles')
            .update({ withdrawal_balance: (user.withdrawal_balance || 0.0) + parseFloat(amount) })
            .eq('id', user.id);
        }
      }
      throw error;
    }
    res.status(201).json({ success: true, transaction: data });
  } catch (error) {
    console.error("Error submitting transaction:", error);
    res.status(500).json({ error: error.message });
  }
});

// Fetch user transaction history
app.get('/api/users/:whatsapp/transactions', async (req, res) => {
  try {
    const { whatsapp } = req.params;
    const { limit } = req.query;
    const queryLimit = parseInt(limit) || 50;

    const { data, error } = await supabase
      .from('transactions')
      .select('*')
      .like('whatsapp_number', `${whatsapp}%`)
      .order('timestamp', { ascending: false })
      .limit(queryLimit);

    if (error) throw error;
    res.json({ success: true, transactions: data });
  } catch (error) {
    console.error("Error fetching transaction history:", error);
    res.status(500).json({ error: error.message });
  }
});


// Fetch paginated referred users for a given WhatsApp number
app.get('/api/users/:whatsapp/referred-users', async (req, res) => {
  try {
    const { whatsapp } = req.params;
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 20;
    const start = (page - 1) * limit;
    const end = start + limit - 1;

    // 1. Fetch user to get their own_referral_code
    const { data: user, error: userError } = await supabase
      .from('profiles')
      .select('own_referral_code')
      .eq('whatsapp_number', whatsapp)
      .single();

    if (userError || !user) {
      return res.status(404).json({ error: "User not found" });
    }

    const code = user.own_referral_code;
    if (!code) {
      return res.json({ success: true, total: 0, users: [] });
    }

    // 2. Fetch total count of referred users
    const { count, error: countErr } = await supabase
      .from('profiles')
      .select('id', { count: 'exact', head: true })
      .eq('referral_code_used', code);

    if (countErr) throw countErr;

    // 3. Fetch referred profiles with pagination
    const { data: profiles, error: profErr } = await supabase
      .from('profiles')
      .select('whatsapp_number, name, created_at')
      .eq('referral_code_used', code)
      .order('created_at', { ascending: false })
      .range(start, end);

    if (profErr) throw profErr;

    // 4. For each profile, fetch total approved deposits
    const usersWithDeposits = [];
    for (const p of (profiles || [])) {
      const { data: txs, error: txsErr } = await supabase
        .from('transactions')
        .select('amount')
        .eq('whatsapp_number', p.whatsapp_number)
        .eq('type', 'DEPOSIT')
        .eq('status', 'APPROVED');

      let totalDeposited = 0.0;
      if (!txsErr && txs) {
        totalDeposited = txs.reduce((sum, t) => sum + (t.amount || 0.0), 0.0);
      }

      // Mask WhatsApp: show only last 4 digits (e.g. ******1234)
      const wa = p.whatsapp_number || "";
      const last4 = wa.length >= 4 ? wa.substring(wa.length - 4) : wa;
      const maskedWa = "*".repeat(Math.max(0, wa.length - 4)) + last4;

      usersWithDeposits.push({
        maskedWhatsapp: maskedWa,
        name: p.name,
        totalDeposited: totalDeposited,
        createdAt: p.created_at
      });
    }

    res.json({
      success: true,
      total: count || 0,
      users: usersWithDeposits
    });
  } catch (error) {
    console.error("Error fetching referred users:", error);
    res.status(500).json({ error: error.message });
  }
});


app.get('/api/admin/stats', async (req, res) => {
  try {
    const { data: users, error: usersErr } = await supabase.from('profiles').select('created_at');
    if (usersErr) throw usersErr;

    const { data: txs, error: txsErr } = await supabase.from('transactions').select('amount, type, status, timestamp').eq('status', 'APPROVED');
    if (txsErr) throw txsErr;

    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];
    
    // Helper to check if a date string is in the current day, week, month
    const isToday = (dateStr) => {
      if (!dateStr) return false;
      return new Date(dateStr).toISOString().split('T')[0] === todayStr;
    };
    
    const getWeekNumber = (d) => {
      d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));
      d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay()||7));
      const yearStart = new Date(Date.UTC(d.getUTCFullYear(),0,1));
      return Math.ceil((((d - yearStart) / 86400000) + 1)/7);
    };
    const currentWeek = getWeekNumber(now);
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();

    const isThisWeek = (dateStr) => {
      if (!dateStr) return false;
      const d = new Date(dateStr);
      return getWeekNumber(d) === currentWeek && d.getFullYear() === currentYear;
    };

    const isThisMonth = (dateStr) => {
      if (!dateStr) return false;
      const d = new Date(dateStr);
      return d.getMonth() === currentMonth && d.getFullYear() === currentYear;
    };

    let totalUsers = users.length;
    let dailyUsers = 0;
    let weeklyUsers = 0;
    let monthlyUsers = 0;

    users.forEach(u => {
      if (isToday(u.created_at)) dailyUsers++;
      if (isThisWeek(u.created_at)) weeklyUsers++;
      if (isThisMonth(u.created_at)) monthlyUsers++;
    });

    let totalSpent = 0;
    let dailySpent = 0;
    let weeklySpent = 0;
    let monthlySpent = 0;
    
    // Graph data grouped by day for the last 7 days
    const last7Days = [];
    for(let i=6; i>=0; i--) {
      const d = new Date(now);
      d.setDate(now.getDate() - i);
      last7Days.push({
        date: d.toISOString().split('T')[0],
        users: 0,
        spent: 0
      });
    }

    txs.forEach(tx => {
      if (tx.type === 'DEPOSIT') {
        const txDate = new Date(tx.timestamp);
        const amt = tx.amount || 0;
        totalSpent += amt;
        if (isToday(txDate)) dailySpent += amt;
        if (isThisWeek(txDate)) weeklySpent += amt;
        if (isThisMonth(txDate)) monthlySpent += amt;
        
        const dStr = txDate.toISOString().split('T')[0];
        const dayItem = last7Days.find(item => item.date === dStr);
        if (dayItem) dayItem.spent += amt;
      }
    });

    users.forEach(u => {
       if (u.created_at) {
          const dStr = new Date(u.created_at).toISOString().split('T')[0];
          const dayItem = last7Days.find(item => item.date === dStr);
          if (dayItem) dayItem.users++;
       }
    });

    res.json({
      success: true,
      stats: {
        totalUsers, dailyUsers, weeklyUsers, monthlyUsers,
        totalSpent, dailySpent, weeklySpent, monthlySpent,
        graphData: last7Days
      }
    });
  } catch (error) {
    console.error("Error fetching stats:", error);
    res.status(500).json({ error: error.message });
  }
});
// Fetch all transactions (Admin dashboard)
app.get('/api/admin/transactions', async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('transactions')
      .select('*')
      .order('timestamp', { ascending: false });

    if (error) throw error;
    res.json({ success: true, transactions: data });
  } catch (error) {
    console.error("Error fetching admin transactions:", error);
    res.status(500).json({ error: error.message });
  }
});

// Approve or Reject Deposit/Withdrawal Transaction (Admin Endpoint)
app.patch('/api/transactions/:id/status', async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body; // "APPROVED" or "REJECTED"

    if (status !== 'APPROVED' && status !== 'REJECTED') {
      return res.status(400).json({ error: "Invalid status. Must be APPROVED or REJECTED." });
    }

    // 1. Fetch transaction
    const { data: tx, error: txError } = await supabase
      .from('transactions')
      .select('*')
      .eq('id', id)
      .single();

    if (txError || !tx) {
      return res.status(404).json({ error: "Transaction not found" });
    }

    if (tx.status !== 'PENDING') {
      return res.status(400).json({ error: `Transaction is already processed (${tx.status})` });
    }

    // 2. Fetch user profile
    const { data: user, error: userError } = await supabase
      .from('profiles')
      .select('*')
      .eq('whatsapp_number', tx.whatsapp_number)
      .single();

    if (userError || !user) {
      return res.status(404).json({ error: "User profile associated with transaction not found" });
    }

    // 3. Process balances if APPROVED or REJECTED
    if (status === 'APPROVED') {
      if (tx.type === 'DEPOSIT') {
        const updatedDeposit = (user.deposit_balance || 0.0) + tx.amount;
        const { error: balanceError } = await supabase
          .from('profiles')
          .update({ deposit_balance: updatedDeposit })
          .eq('id', user.id);

        if (balanceError) throw balanceError;

        // Process referral reward if user used a referral code and deposit >= min_deposit
        if (user.referral_code_used) {
          const settings = getGlobalSettings();
          const minDeposit = settings.referral_min_deposit !== undefined ? parseFloat(settings.referral_min_deposit) : 20.0;
          if (tx.amount >= minDeposit) {
            await handleReferralReward(user.referral_code_used, tx.whatsapp_number);
          }
        }
      }
      // For WITHDRAWAL, the balance was already deducted upon submission.
    } else if (status === 'REJECTED') {
      if (tx.type === 'WITHDRAWAL') {
        // Refund the withdrawn amount back to the user's withdrawal_balance
        const updatedWithdrawal = (user.withdrawal_balance || 0.0) + tx.amount;
        const { error: balanceError } = await supabase
          .from('profiles')
          .update({ withdrawal_balance: updatedWithdrawal })
          .eq('id', user.id);

        if (balanceError) throw balanceError;
      }
    }

    // 4. Update transaction status
    const { data: updatedTx, error: updateTxError } = await supabase
      .from('transactions')
      .update({ status })
      .eq('id', id)
      .select()
      .single();

    if (updateTxError) throw updateTxError;

    res.json({ success: true, transaction: updatedTx });
  } catch (error) {
    console.error("Error processing transaction approval:", error);
    res.status(500).json({ error: error.message });
  }
});


// ==========================================
// GAME HISTORY & RECORDS ENDPOINTS
// ==========================================

// Get game histories for user
app.get('/api/users/:whatsapp/game-histories', async (req, res) => {
  try {
    const { whatsapp } = req.params;
    const { data, error } = await supabase
      .from('game_histories')
      .select('*')
      .like('whatsapp_number', `${whatsapp}%`)
      .order('timestamp', { ascending: false });

    if (error) throw error;
    res.json({ success: true, gameHistories: data });
  } catch (error) {
    console.error("Error fetching user game history:", error);
    res.status(500).json({ error: error.message });
  }
});

// Get all game histories (Admin Only)
app.get('/api/admin/game-histories', async (req, res) => {
  try {
    const { data, error } = await supabase
      .from('game_histories')
      .select('*')
      .order('timestamp', { ascending: false });

    if (error) throw error;
    res.json({ success: true, gameHistories: data });
  } catch (error) {
    console.error("Error fetching admin game histories:", error);
    res.status(500).json({ error: error.message });
  }
});

// Update game history status and award prizes automatically (Admin Endpoint)
app.patch('/api/admin/game-histories/:id/status', async (req, res) => {
  try {
    const { id } = req.params;
    const { status, prize_won } = req.body; // status: 'COMPLETED', prize_won: numeric

    if (!status) {
      return res.status(400).json({ error: "status is a required field" });
    }

    // 1. Fetch existing game history
    const { data: history, error: historyError } = await supabase
      .from('game_histories')
      .select('*')
      .eq('id', id)
      .single();

    if (historyError || !history) {
      return res.status(404).json({ error: "Game history record not found" });
    }

    if (history.status === 'COMPLETED') {
      return res.status(400).json({ error: "Game history record is already completed" });
    }

    const prize = parseFloat(prize_won) || 0.0;

    // 2. Award prize if greater than 0
    if (prize > 0) {
      // Fetch user profile
      const { data: user, error: userError } = await supabase
        .from('profiles')
        .select('*')
        .eq('whatsapp_number', history.whatsapp_number)
        .single();

      if (userError || !user) {
        return res.status(404).json({ error: `User profile associated with game history not found: ${history.whatsapp_number}` });
      }

      // Add to withdrawal balance
      const newWithdrawalBalance = (user.withdrawal_balance || 0.0) + prize;
      const { error: balanceError } = await supabase
        .from('profiles')
        .update({ withdrawal_balance: newWithdrawalBalance })
        .eq('id', user.id);

      if (balanceError) throw balanceError;

      // Log transaction of prize won
      await supabase
        .from('transactions')
        .insert([
          {
            whatsapp_number: history.whatsapp_number,
            type: 'PRIZE_WON',
            amount: prize,
            upi_id: 'GAME_PRIZE',
            reference_number: `GAME-${id}`,
            status: 'APPROVED',
            timestamp: Date.now()
          }
        ]);
    }

    // 3. Update game history status
    const { data: updatedHistory, error: updateError } = await supabase
      .from('game_histories')
      .update({
        status,
        prize_won: prize > 0 ? prize : null
      })
      .eq('id', id)
      .select()
      .single();

    if (updateError) throw updateError;

    res.json({ success: true, gameHistory: updatedHistory });
  } catch (error) {
    console.error("Error updating game history status:", error);
    res.status(500).json({ error: error.message });
  }
});


// ==========================================
// CONFIGURATION MANAGEMENT
// ==========================================
const fs = require('fs');
const path = require('path');
const configFilePath = path.join(__dirname, 'app_settings.json');
const oldUpiFilePath = path.join(__dirname, 'upi_settings.json');

function getGlobalSettings() {
  let settings = {
    upi_id: "pay.arenaesports@upi",
    wa_url: "https://wa.me/919999999999",
    tg_url: "https://t.me/arenaesportssupport",
    referral_reward: 50.0,
    referral_min_deposit: 20.0
  };
  try {
    if (fs.existsSync(configFilePath)) {
      const fileData = fs.readFileSync(configFilePath, 'utf8');
      settings = { ...settings, ...JSON.parse(fileData) };
    } else if (fs.existsSync(oldUpiFilePath)) {
      const fileData = fs.readFileSync(oldUpiFilePath, 'utf8');
      const config = JSON.parse(fileData);
      if (config.upi_id) settings.upi_id = config.upi_id;
    }
  } catch (e) {
    console.error("Error reading settings:", e);
  }
  return settings;
}

function saveGlobalSettings(newSettings) {
  try {
    const settings = getGlobalSettings();
    const updatedSettings = { ...settings, ...newSettings };
    fs.writeFileSync(configFilePath, JSON.stringify(updatedSettings), 'utf8');
    return true;
  } catch (e) {
    console.error("Error writing settings:", e);
    return false;
  }
}

// Get current config
app.get('/api/settings', (req, res) => {
  res.json({ success: true, settings: getGlobalSettings() });
});

// Update global config (Admin)
app.post('/api/settings', (req, res) => {
  const { upi_id, wa_url, tg_url, referral_reward, referral_min_deposit } = req.body;
  const updates = {};
  if (upi_id && upi_id.trim().length > 0) updates.upi_id = upi_id.trim();
  if (wa_url && wa_url.trim().length > 0) updates.wa_url = wa_url.trim();
  if (tg_url && tg_url.trim().length > 0) updates.tg_url = tg_url.trim();
  if (referral_reward !== undefined) updates.referral_reward = parseFloat(referral_reward) || 0;
  if (referral_min_deposit !== undefined) updates.referral_min_deposit = parseFloat(referral_min_deposit) || 0;
  
  const success = saveGlobalSettings(updates);
  if (success) {
    res.json({ success: true, settings: getGlobalSettings() });
  } else {
    res.status(500).json({ error: "Failed to persist settings on server" });
  }
});

// Legacy Get current UPI ID (for backwards compatibility if needed)
app.get('/api/upi', (req, res) => {
  res.json({ success: true, upi_id: getGlobalSettings().upi_id });
});

// Legacy Update global UPI ID (Admin)
app.post('/api/upi', (req, res) => {
  const { upi_id } = req.body;
  if (!upi_id || upi_id.trim().length === 0) {
    return res.status(400).json({ error: "UPI ID is required" });
  }
  const success = saveGlobalSettings({ upi_id: upi_id.trim() });
  if (success) {
    res.json({ success: true, upi_id: upi_id.trim() });
  } else {
    res.status(500).json({ error: "Failed to persist UPI ID on server" });
  }
});


// ==========================================
// ADDITIONAL ADMIN PANEL ENDPOINTS
// ==========================================

// Search/Get profile by whatsapp (Admin)
app.get('/api/admin/users/:whatsapp', async (req, res) => {
  try {
    const { whatsapp } = req.params;
    const { data, error } = await supabase
      .from('profiles')
      .select('*')
      .like('whatsapp_number', `${whatsapp}%`)
      .maybeSingle();

    if (error) throw error;
    if (!data) {
      return res.status(404).json({ error: "User profile not found" });
    }
    res.json({ success: true, user: data });
  } catch (error) {
    console.error("Error searching user:", error);
    res.status(500).json({ error: error.message });
  }
});

// Update user balance directly (Admin)
app.post('/api/admin/users/:whatsapp/balance', async (req, res) => {
  try {
    const { whatsapp } = req.params;
    const { deposit_balance, withdrawal_balance } = req.body;

    // Fetch profile first
    const { data: user, error: userError } = await supabase
      .from('profiles')
      .select('*')
      .like('whatsapp_number', `${whatsapp}%`)
      .single();

    if (userError || !user) {
      return res.status(404).json({ error: "User profile not found" });
    }

    const updatedObj = {};
    if (deposit_balance !== undefined) {
      updatedObj.deposit_balance = parseFloat(deposit_balance);
    }
    if (withdrawal_balance !== undefined) {
      updatedObj.withdrawal_balance = parseFloat(withdrawal_balance);
    }

    const { data: updatedUser, error: updateError } = await supabase
      .from('profiles')
      .update(updatedObj)
      .eq('id', user.id)
      .select()
      .single();

    if (updateError) throw updateError;

    // Create a transaction log of the adjustment
    await supabase
      .from('transactions')
      .insert([
        {
          whatsapp_number: whatsapp,
          type: 'BALANCE_ADJUST',
          amount: Math.abs((deposit_balance !== undefined ? (deposit_balance - user.deposit_balance) : 0.0) + 
                           (withdrawal_balance !== undefined ? (withdrawal_balance - user.withdrawal_balance) : 0.0)),
          upi_id: 'ADMIN_ADJUST',
          reference_number: `ADJ-${Date.now().toString().slice(-6)}`,
          status: 'APPROVED',
          timestamp: Date.now()
        }
      ]);

    res.json({ success: true, user: updatedUser });
  } catch (error) {
    console.error("Error updating balance:", error);
    res.status(500).json({ error: error.message });
  }
});

// Get registrations for a tournament (Admin)
app.get('/api/admin/tournaments/:id/registrations', async (req, res) => {
  try {
    const { id } = req.params;
    const { data, error } = await supabase
      .from('registrations')
      .select('*')
      .eq('tournament_id', id);

    if (error) throw error;
    res.json({ success: true, registrations: data });
  } catch (error) {
    console.error("Error fetching tournament registrations:", error);
    res.status(500).json({ error: error.message });
  }
});

// Update a registration record (Admin)
app.patch('/api/admin/registrations/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { whatsapp_number } = req.body; // updated formatted string

    const { data, error } = await supabase
      .from('registrations')
      .update({ whatsapp_number })
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;
    res.json({ success: true, registration: data });
  } catch (error) {
    console.error("Error updating registration:", error);
    res.status(500).json({ error: error.message });
  }
});

// Declare position & Reward team (Admin)
app.post('/api/admin/registrations/:id/reward', async (req, res) => {
  try {
    const { id } = req.params;
    const { position, prize_amount, raw_whatsapp, tournament_title } = req.body;

    if (!raw_whatsapp || prize_amount === undefined) {
      return res.status(400).json({ error: "raw_whatsapp and prize_amount are required" });
    }

    // 0. Fetch registration to get tournament_id
    const { data: reg } = await supabase
      .from('registrations')
      .select('*')
      .eq('id', id)
      .single();

    const tId = reg ? reg.tournament_id : '';
    const finalGameName = `${tournament_title || 'Tournament'}|tourId:${tId}|pos:${position || 'Completed'}`;

    // 1. Fetch user profile
    const { data: user, error: userError } = await supabase
      .from('profiles')
      .select('*')
      .eq('whatsapp_number', raw_whatsapp)
      .single();

    if (userError || !user) {
      return res.status(404).json({ error: "User profile associated with registration not found" });
    }

    const prize = parseFloat(prize_amount) || 0.0;

    // 2. Award prize if greater than 0 (add to withdrawal_balance)
    if (prize > 0) {
      const newWithdrawalBalance = (user.withdrawal_balance || 0.0) + prize;
      const { error: balanceError } = await supabase
        .from('profiles')
        .update({ withdrawal_balance: newWithdrawalBalance })
        .eq('id', user.id);

      if (balanceError) throw balanceError;

      // Log transaction of prize won
      await supabase
        .from('transactions')
        .insert([
          {
            whatsapp_number: raw_whatsapp,
            type: 'PRIZE_WON',
            amount: prize,
            upi_id: 'TOURNAMENT_REWARD',
            reference_number: `TOUR-${id}`,
            status: 'APPROVED',
            timestamp: Date.now()
          }
        ]);
    }

    // 3. Insert or complete a record in game_histories
    // Find any existing game history for this user and game
    const { data: history } = await supabase
      .from('game_histories')
      .select('*')
      .eq('whatsapp_number', raw_whatsapp)
      .eq('status', 'PENDING')
      .limit(1)
      .maybeSingle();

    if (history) {
      await supabase
        .from('game_histories')
        .update({
          status: 'COMPLETED',
          game_name: finalGameName,
          prize_won: prize > 0 ? prize : null
        })
        .eq('id', history.id);
    } else {
      await supabase
        .from('game_histories')
        .insert([
          {
            whatsapp_number: raw_whatsapp,
            game_name: finalGameName,
            prize_won: prize > 0 ? prize : null,
            status: 'COMPLETED',
            timestamp: Date.now()
          }
        ]);
    }

    // Update registration with position and prize inside whatsapp_number column
    if (reg) {
      const parts = reg.whatsapp_number.split("|");
      const baseNum = parts[0] || "";
      const teamName = parts[1] || "";
      const members = parts[2] || "";
      const slotNum = parts[3] || "";
      const updatedWhatsappNum = `${baseNum}|${teamName}|${members}|${slotNum}|${position || "Completed"}|${prize_amount || "0"}`;

      await supabase
        .from('registrations')
        .update({ whatsapp_number: updatedWhatsappNum })
        .eq('id', id);
    }


    res.json({ success: true, message: `Position ${position} declared and reward of ${prize} awarded to ${raw_whatsapp}` });
  } catch (error) {
    console.error("Error declaring position & reward:", error);
    res.status(500).json({ error: error.message });
  }
});

// Update complete Tournament details (Admin)
app.patch('/api/tournaments/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { game, title, poster_res, entry_fee, prize_pool, prize_1st, prize_2nd, prize_3rd, prize_4th, rules, start_time } = req.body;

    const updateObj = {};
    if (game !== undefined) updateObj.game = game;
    if (title !== undefined) updateObj.title = title;
    if (poster_res !== undefined) updateObj.poster_res = poster_res;
    if (entry_fee !== undefined) updateObj.entry_fee = parseFloat(entry_fee);
    if (prize_pool !== undefined) updateObj.prize_pool = parseFloat(prize_pool);
    if (prize_1st !== undefined) updateObj.prize_1st = parseFloat(prize_1st);
    if (prize_2nd !== undefined) updateObj.prize_2nd = parseFloat(prize_2nd);
    if (prize_3rd !== undefined) updateObj.prize_3rd = parseFloat(prize_3rd);
    if (prize_4th !== undefined) updateObj.prize_4th = parseFloat(prize_4th);
    if (rules !== undefined) updateObj.rules = rules;
    if (start_time !== undefined) updateObj.start_time = start_time;

    const { data, error } = await supabase
      .from('tournaments')
      .update(updateObj)
      .eq('id', id)
      .select()
      .single();

    if (error) throw error;

    res.json({ success: true, tournament: data });
  } catch (error) {
    console.error("Error updating tournament details:", error);
    res.status(500).json({ error: error.message });
  }
});


// ==========================================
// REFERRAL MECHANICS HELPER
// ==========================================
async function handleReferralReward(referrerCode, referredWhatsapp) {
  try {
    // Check if we have already rewarded this referral
    const { data: existingTx, error: txCheckErr } = await supabase
      .from('transactions')
      .select('id')
      .eq('type', 'REFERRAL_REWARD')
      .eq('reference_number', `REF-${referredWhatsapp}`)
      .maybeSingle();

    if (existingTx) {
      console.log(`Referral reward already processed for referring ${referredWhatsapp}`);
      return;
    }

    // 1. Fetch referrer profile
    const { data: referrer, error: refError } = await supabase
      .from('profiles')
      .select('*')
      .eq('own_referral_code', referrerCode)
      .maybeSingle();

    if (refError || !referrer) {
      console.warn(`Referrer not found for code: ${referrerCode}`);
      return;
    }

    const currentDeposit = referrer.deposit_balance || 0.0;
    const currentReferredCount = referrer.referred_count || 0;

    const settings = getGlobalSettings();
    const rewardAmount = settings.referral_reward !== undefined ? parseFloat(settings.referral_reward) : 50.0;

    // 2. Update referrer balances & count
    const { error: updateError } = await supabase
      .from('profiles')
      .update({
        deposit_balance: currentDeposit + rewardAmount,
        referred_count: currentReferredCount + 1
      })
      .eq('id', referrer.id);

    if (updateError) throw updateError;

    // 3. Insert transaction log for reward
    await supabase
      .from('transactions')
      .insert([
        {
          whatsapp_number: referrer.whatsapp_number,
          type: 'REFERRAL_REWARD',
          amount: rewardAmount,
          upi_id: 'REFERRAL',
          reference_number: `REF-${referredWhatsapp}`,
          status: 'APPROVED',
          timestamp: Date.now()
        }
      ]);

    console.log(`Successfully rewarded ${referrer.whatsapp_number} for referring ${referredWhatsapp}`);
  } catch (err) {
    console.error("Failed to process referral reward:", err);
  }
}


// Start Server
app.listen(PORT, () => {
  console.log(`=================================================`);
  console.log(`Gamer Tournament Server running on port ${PORT}`);
  console.log(`URL: http://localhost:${PORT}`);
  console.log(`=================================================`);
});
