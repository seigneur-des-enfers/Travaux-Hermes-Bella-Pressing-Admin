const fs = require('fs');

let dash = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

dash = dash.replace(
    /                    if \(order\.status == "EN_ATTENTE" && canManageOrders\) \{\n                        Spacer\(modifier = Modifier\.height\(16\.dp\)\)\n                        Row\(modifier = Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)\) \{\n                            Button\(\n                                onClick = \{ viewModel\.updateOrder\(order\.copy\(status = "ACCEPTE", timelineStep = 1\)\) \},\n                                modifier = Modifier\.weight\(1f\)\n                            \) \{\n                                Text\("Accepté"\)\n                            \}/,
    `                    if (order.status == "EN_ATTENTE" && canManageOrders) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = kgInput,
                            onValueChange = { kgInput = it },
                            label = { Text("Poids après pesée (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    val k = kgInput.toFloatOrNull() ?: order.kg
                                    var calcPrice = calculateOrderPrice(
                                        pieces = order.pieces,
                                        kg = k,
                                        lavageType = order.lavageType,
                                        gommagePaire = order.gommagePaire,
                                        gommageImpair = order.gommageImpair,
                                        config = config,
                                        isExpress = order.isExpress,
                                        isPremium = order.hasPremiumGuarantee
                                    )
                                    calcPrice += order.specialItemsData.split(",").filter { it.isNotBlank() }.mapNotNull { 
                                        val parts = it.split(":")
                                        if (parts.size == 2) {
                                            val p = specialItems.find { item -> item.name == parts[0] }?.price ?: 0f
                                            p * (parts[1].toIntOrNull() ?: 0)
                                        } else null
                                    }.sum()
                                    viewModel.updateOrder(order.copy(status = "ACCEPTE", timelineStep = 1, kg = k, prixFinal = calcPrice))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Accepté")
                            }`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', dash);
console.log("Kg input fixed in AdminDashboard");
