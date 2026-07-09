# Gamer Tournament Backend Server (Render + Supabase)

This directory contains the full backend code for your Gamer Tournament application. It is a high-performance **Node.js + Express** server pre-configured to handle:
- **User authentication** (secure signup and login synced with Supabase Auth).
- **User profiles & referral mechanics** (automatically rewarding referrers and tracking invite counts).
- **Tournaments** (fetching, adding, and updating games/events).
- **Secure registration checkouts** (deducting entry fees, managing combined wallet balances, and starting game histories).
- **Transaction reviews** (secure API handlers for admin approval/rejection of deposits & withdrawals).
- **Game History & automated prize distributions** (approving match results automatically credits players' withdrawal balances and logs transactions).

---

## 🛠️ Part 1: Setting up your Database on Supabase

Since this server (and your Android app) works directly with Supabase, you **must run the following SQL code** in your Supabase Project to create the required tables and configure database access.

### Instructions:
1. Log in to your [Supabase Dashboard](https://supabase.com).
2. Open your project.
3. Click on the **SQL Editor** tab in the left sidebar (looks like `>_`).
4. Click **New Query**, paste the code below, and click **Run**.

```sql
-- 1. Create Profiles Table (Users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY,
    whatsapp_number TEXT UNIQUE NOT NULL,
    name TEXT,
    own_referral_code TEXT UNIQUE NOT NULL,
    referral_code_used TEXT,
    deposit_balance DOUBLE PRECISION DEFAULT 0.0,
    withdrawal_balance DOUBLE PRECISION DEFAULT 0.0,
    referred_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Create Tournaments Table
CREATE TABLE IF NOT EXISTS public.tournaments (
    id BIGSERIAL PRIMARY KEY,
    game TEXT NOT NULL, -- 'BGMI', 'PUBG', 'FREEFIRE', etc.
    title TEXT NOT NULL,
    poster_res TEXT NOT NULL,
    entry_fee DOUBLE PRECISION DEFAULT 0.0,
    prize_pool DOUBLE PRECISION DEFAULT 0.0,
    prize_1st DOUBLE PRECISION DEFAULT 0.0,
    prize_2nd DOUBLE PRECISION DEFAULT 0.0,
    prize_3rd DOUBLE PRECISION DEFAULT 0.0,
    prize_4th DOUBLE PRECISION DEFAULT 0.0,
    rules TEXT,
    room_id TEXT,
    room_password TEXT,
    start_time TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. Create Registrations Table
CREATE TABLE IF NOT EXISTS public.registrations (
    id BIGSERIAL PRIMARY KEY,
    whatsapp_number TEXT NOT NULL,
    tournament_id BIGINT REFERENCES public.tournaments(id) ON DELETE CASCADE,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. Create Transactions Table
CREATE TABLE IF NOT EXISTS public.transactions (
    id BIGSERIAL PRIMARY KEY,
    whatsapp_number TEXT NOT NULL,
    type TEXT NOT NULL, -- 'DEPOSIT', 'WITHDRAWAL', 'REFERRAL_REWARD', 'PRIZE_WON'
    amount DOUBLE PRECISION DEFAULT 0.0,
    upi_id TEXT,
    reference_number TEXT,
    status TEXT DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. Create Game Histories Table
CREATE TABLE IF NOT EXISTS public.game_histories (
    id BIGSERIAL PRIMARY KEY,
    whatsapp_number TEXT NOT NULL,
    game_name TEXT NOT NULL,
    prize_won DOUBLE PRECISION,
    status TEXT DEFAULT 'PENDING', -- 'PENDING', 'COMPLETED'
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 6. Turn off Row Level Security (RLS) for Development/Direct Android API access
-- This ensures that your client-side Android application does not encounter permission/auth policy blocks!
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.tournaments DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.registrations DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.game_histories DISABLE ROW LEVEL SECURITY;
```

---

## 🚀 Part 2: Hosting the Server on Render

[Render](https://render.com) is a free cloud platform perfect for deploying Express apps. Follow these steps to host your server for free:

### Step 1: Push your code to GitHub
If you haven't already, push this `/server` directory to your GitHub account:
1. Initialize a git repository if needed: `git init`
2. Commit files: `git add . && git commit -m "Add tournament backend server"`
3. Push to your GitHub repository.

### Step 2: Deploy on Render
1. Go to [Render Dashboard](https://dashboard.render.com) and create a free account.
2. Click **New +** on the top right and select **Web Service**.
3. Connect your GitHub account and select your repository containing this code.
4. Fill in the service details:
   - **Name**: `gamer-tournament-api`
   - **Environment**: `Node`
   - **Region**: Select the closest one to your users
   - **Branch**: `main` (or `master`)
   - **Root Directory**: `server` (Important! This directs Render to look inside this specific sub-folder)
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
   - **Instance Type**: **Free** ($0/month)

### Step 3: Add Environment Variables on Render
During setup or inside the **Environment** tab of your Render service, add the following variables:

| Key | Value | Description |
|---|---|---|
| `SUPABASE_URL` | `https://your-project.supabase.co` | Your Supabase Project URL |
| `SUPABASE_SERVICE_ROLE_KEY` | `your-service-role-secret-key` | Your private Service Role key (found in Supabase under settings -> API). It starts with `eyJ...`. **Do not share this key!** |
| `PORT` | `10000` | Port for the app to listen on (Render sets this automatically, but good to have) |

Click **Save Changes** and Render will automatically build and deploy your live server! Once finished, Render will provide a live API URL like `https://gamer-tournament-api.onrender.com`.

---

## 📡 Complete API Reference Endpoints

Your newly deployed server exposes the following endpoints:

### 🔐 Authentication APIs:
- `POST /api/auth/signup` - Registers a user with their WhatsApp number & password, auto-generates their unique referral code, establishes their database profile, and automatically applies credit rewards if they used someone else's referral code.
- `POST /api/auth/login` - Signs the user in and returns the current profile data and access token.

### 👤 Profile & User APIs:
- `GET /api/users/:whatsapp` - Retrieves user details, referral codes, and wallets.
- `GET /api/users/:whatsapp/registrations` - Retrieves all tournaments registered by the user.

### 🎮 Tournament APIs:
- `GET /api/tournaments` - Fetches the full active tournaments board.
- `GET /api/tournaments/:id` - Fetches details for a single tournament.
- `POST /api/tournaments` - **(Admin Only)** Creates a new tournament.
- `PATCH /api/tournaments/:id/room` - **(Admin Only)** Sets the custom Room ID, Room Password, or Start Time for players to join the match.
- `DELETE /api/tournaments/:id` - **(Admin Only)** Deletes a tournament completely.

### 🎫 Booking/Registrations APIs:
- `POST /api/tournaments/register` - Safely registers a user for a tournament, verifying remaining combined wallet balances, debiting entry fees (prioritizing deposits first, then withdrawals), creating a registration log, and initializing a pending game history record.

### 💳 Wallet & Transaction APIs:
- `POST /api/transactions` - Submits a deposit/withdrawal request (places it in a `PENDING` queue).
- `GET /api/users/:whatsapp/transactions` - Retrieves the transaction logs for a user.
- `GET /api/admin/transactions` - **(Admin Only)** Retrieves all transaction requests across the entire platform.
- `PATCH /api/transactions/:id/status` - **(Admin Only)** Approves or Rejects transaction requests. Approvals automatically update player balances.

### 🏆 Match Records & Game Histories:
- `GET /api/users/:whatsapp/game-histories` - Retrieves game play history and prize claims for a user.
- `GET /api/admin/game-histories` - **(Admin Only)** Retrieves all match lists and completion queues.
- `PATCH /api/admin/game-histories/:id/status` - **(Admin Only)** Sets status of matches (e.g. `COMPLETED`) and specifies prize claims. If a user won a cash prize, **the server automatically credits their withdrawal balance** and records a logged transaction!
