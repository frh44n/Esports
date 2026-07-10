
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
