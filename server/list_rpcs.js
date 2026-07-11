const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SECRET_KEY || process.env.SUPABASE_SERVICE_ROLE_KEY;

const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

async function main() {
  // Query RPCs from postgres schema (usually requires RLS bypass, which service_role key does)
  const { data, error } = await supabase.from('pg_proc').select('proname').limit(10);
  console.log("pg_proc:", data, error);
}
main();
