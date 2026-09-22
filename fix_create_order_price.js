const fs = require('fs');

let createOrder = fs.readFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', 'utf8');

createOrder = createOrder.replace(
/val finalPriceComputed = maxOf\(0f, \(\(basePrix \+ insuranceFee\) \* expressMultiplier \* \(1f - finalDiscountPercentage\)\) - finalFixedDiscount\) \+ transport/,
`val rawPrice = maxOf(0f, ((basePrix + insuranceFee) * expressMultiplier * (1f - finalDiscountPercentage)) - finalFixedDiscount) + transport
                    val finalPriceComputed = Math.round(rawPrice).toFloat()`
);

fs.writeFileSync('./app/src/main/java/com/example/ui/screens/CreateOrderScreen.kt', createOrder);
console.log("CreateOrderScreen price fixed");
