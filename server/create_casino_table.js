const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SECRET_KEY;

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

async function main() {
  console.log("Checking if casino_games table exists or needs to be created...");
  
  // SQL to create the table if not exists and populate default values
  const sql = `
    CREATE TABLE IF NOT EXISTS casino_games (
      id SERIAL PRIMARY KEY,
      name TEXT NOT NULL,
      poster_url TEXT NOT NULL,
      is_active BOOLEAN DEFAULT TRUE,
      created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
    );
  `;
  
  const { data, error } = await supabase.rpc('query', { query: sql });
  if (error) {
    console.error("Error creating casino_games table:", error);
    return;
  }
  console.log("casino_games table created or verified successfully:", data);

  // Let's check if there are any casino games
  const { data: existingGames, error: fetchErr } = await supabase
    .from('casino_games')
    .select('*');

  if (fetchErr) {
    console.error("Error checking existing casino games:", fetchErr);
    return;
  }

  if (existingGames.length === 0) {
    console.log("Inserting default casino games...");
    const defaultGames = [
      {
        name: "Ludo Classic",
        poster_url: "https://images.unsplash.com/photo-1611195974226-a6a9be9dd763?auto=format&fit=crop&w=600&q=80"
      },
      {
        name: "Mines Sweeper",
        poster_url: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80"
      }
    ];

    const { data: insertData, error: insertErr } = await supabase
      .from('casino_games')
      .insert(defaultGames)
      .select();

    if (insertErr) {
      console.error("Error inserting default casino games:", insertErr);
    } else {
      console.log("Default casino games inserted successfully:", insertData);
    }
  } else {
    console.log("Casino games already exist in the database:", existingGames);
  }
}

main();
