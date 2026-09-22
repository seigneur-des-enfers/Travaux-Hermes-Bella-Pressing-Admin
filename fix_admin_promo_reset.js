const fs = require('fs');
let code = fs.readFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', 'utf8');

const target = `                    showPromoDialog = false
                    promoCodeInput = ""
                    promoDescInput = ""
                    promoDiscountInput = ""
                    promoFreeKgInput = ""
                    promoDurationDays = ""
                    promoDurationHours = ""`;

const replacement = `                    showPromoDialog = false
                    promoCodeInput = ""
                    promoDescInput = ""
                    promoDiscountInput = ""
                    promoFreeKgInput = ""
                    promoDurationDays = ""
                    promoDurationHours = ""
                    promoTargetItems = "ALL"
                    promoDiscountType = "PERCENTAGE"
                    promoIsPermanent = true`;

code = code.replace(target, replacement);
fs.writeFileSync('./app/src/main/java/com/example/ui/screens/AdminDashboard.kt', code);
console.log("AdminDashboard promo state reset fixed");
