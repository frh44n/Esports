const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://esports-13p1.onrender.com'; // or from process.env
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SECRET_KEY || process.env.SUPABASE_SERVICE_ROLE_KEY;

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

async function main() {
  console.log("Running alter table query to add status column...");
  const { data, error } = await supabase.rpc('query', { 
    query: "ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'UPCOMING';" 
  });
  if (error) {
    console.error("Error executing query RPC:", error);
  } else {
    console.log("Alter table query ran successfully. Result:", data);
  }
}
main();
