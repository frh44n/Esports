require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { createClient } = require('@supabase/supabase-js');

const app = express();
const PORT = process.env.PORT || 10000;

// Enable CORS and JSON parsing
app.use(cors());
app.use(express.json());

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
    const { whatsapp, name, password, referral_code } = req.body;
    if (!whatsapp || !password) {
      return res.status(400).json({ error: "WhatsApp number and password are required" });
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

    // 3. Trigger Referral Reward if code was used
    if (referral_code) {
      await handleReferralReward(referral_code, whatsapp);
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

// Delete Tournament (Admin)
app.delete('/api/tournaments/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { error } = await supabase
      .from('tournaments')
      .delete()
      .eq('id', id);

    if (error) throw error;
    res.json({ success: true, message: `Tournament ${id} deleted successfully` });
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

    // 5. Insert to Game History (as pending, using raw_whatsapp)
    await supabase
      .from('game_histories')
      .insert([
        {
          whatsapp_number: raw_whatsapp,
          game_name: tournament.game,
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
// UPI ID MANAGEMENT & CONFIGURATION
// ==========================================
const fs = require('fs');
const path = require('path');
const upiFilePath = path.join(__dirname, 'upi_settings.json');

function getGlobalUpiId() {
  try {
    if (fs.existsSync(upiFilePath)) {
      const fileData = fs.readFileSync(upiFilePath, 'utf8');
      const config = JSON.parse(fileData);
      if (config.upi_id) return config.upi_id;
    }
  } catch (e) {
    console.error("Error reading UPI settings:", e);
  }
  return "pay.arenaesports@upi"; // default
}

function saveGlobalUpiId(upiId) {
  try {
    fs.writeFileSync(upiFilePath, JSON.stringify({ upi_id: upiId }), 'utf8');
    return true;
  } catch (e) {
    console.error("Error writing UPI settings:", e);
    return false;
  }
}

// Get current UPI ID
app.get('/api/upi', (req, res) => {
  res.json({ success: true, upi_id: getGlobalUpiId() });
});

// Update global UPI ID (Admin)
app.post('/api/upi', (req, res) => {
  const { upi_id } = req.body;
  if (!upi_id || upi_id.trim().length === 0) {
    return res.status(400).json({ error: "UPI ID is required" });
  }
  const success = saveGlobalUpiId(upi_id.trim());
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
          prize_won: prize > 0 ? prize : null
        })
        .eq('id', history.id);
    } else {
      await supabase
        .from('game_histories')
        .insert([
          {
            whatsapp_number: raw_whatsapp,
            game_name: tournament_title || 'Tournament',
            prize_won: prize > 0 ? prize : null,
            status: 'COMPLETED',
            timestamp: Date.now()
          }
        ]);
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

    // 2. Update referrer balances & count
    const { error: updateError } = await supabase
      .from('profiles')
      .update({
        deposit_balance: currentDeposit + 50.0, // 50 credits reward
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
          amount: 50.0,
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
