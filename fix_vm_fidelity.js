const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', 'utf8');

const target = `            if (order.status == "TERMINE" && order.kg > 0) {
                // Update fidelity points
                val user = repository.getUserById(order.userId)
                if (user != null) {
                    val updatedPoints = user.fidelityPointsKg + order.kg
                    repository.updateUser(user.copy(fidelityPointsKg = updatedPoints))
                    
                    // Refresh current user if it's the logged in one
                    if (_currentUser.value?.id == user.id) {
                        _currentUser.value = user.copy(fidelityPointsKg = updatedPoints)
                    }
                }
            }`;

const replacement = `            if (order.status == "TERMINE" && order.kg > 0) {
                // Update fidelity points
                val user = repository.getUserById(order.userId)
                val config = _config.value
                val globalLoyaltyEnabled = config?.isLoyaltyEnabled == true && (config.loyaltyValidUntil == null || config.loyaltyValidUntil > System.currentTimeMillis())
                
                if (user != null && globalLoyaltyEnabled) {
                    val userLoyaltyEnabled = user.isLoyaltyEnabled && (user.loyaltyValidUntil == null || user.loyaltyValidUntil > System.currentTimeMillis())
                    if (userLoyaltyEnabled) {
                        val updatedPoints = user.fidelityPointsKg + order.kg
                        repository.updateUser(user.copy(fidelityPointsKg = updatedPoints))
                        
                        // Refresh current user if it's the logged in one
                        if (_currentUser.value?.id == user.id) {
                            _currentUser.value = user.copy(fidelityPointsKg = updatedPoints)
                        }
                    }
                }
            }`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/AppViewModel.kt', code);
console.log("AppViewModel fidelity patched");
