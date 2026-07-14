cat << 'INNER' > patch.py
import re

with open('app/src/main/java/com/example/ui/AppUi.kt', 'r') as f:
    content = f.read()

target = "@Composable fun AdminPanelScreen(viewModel: MainViewModel) {}"

replacement = """@Composable
fun AdminPanelScreen(viewModel: MainViewModel) {
    val ludoRequests by viewModel.ludoMatchRequests.collectAsStateWithLifecycle()
    val adminActiveTournament by viewModel.adminActiveLudoTournament.collectAsStateWithLifecycle()
    val adminActiveOpponentName by viewModel.adminActiveLudoOpponentName.collectAsStateWithLifecycle()

    if (adminActiveTournament != null) {
        LudoGameManager(
            viewModel = viewModel,
            tournament = adminActiveTournament!!,
            opponentName = adminActiveOpponentName,
            onBack = {
                viewModel.endAdminLudoMatch()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        TopAppBar(
            title = { Text("Admin Panel", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = { viewModel.setScreen("home") }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Live Ludo Players",
                    color = PurpleGlow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (ludoRequests.isEmpty()) {
                item {
                    Text("No live players currently waiting.", color = Color.Gray)
                }
            } else {
                items(ludoRequests) { req ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(req.userName, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Waiting for match...", color = CyanGlow, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.adminJoinLudoMatch(req) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGlow)
                            ) {
                                Text("Join Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/AppUi.kt', 'w') as f:
    f.write(content)
INNER
python3 patch.py
