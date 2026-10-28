package com.example.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.UUID

@Keep
@IgnoreExtraProperties
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val role: String = "CLIENT", // "CLIENT", "ADMIN", "SUPER_ADMIN"
    val nom: String = "",
    val prenom: String = "",
    val email: String = "",
    val telephone: String = "",
    val motDePasse: String = "",
    val coordonnees: String = "",
    val token: String = UUID.randomUUID().toString(),
    val fidelityPointsKg: Float = 0f,
    val myReferralCode: String = UUID.randomUUID().toString().substring(0, 8).uppercase(),
    val referredBy: String = "",
    val isSuspended: Boolean = false,
    val canUsePromo: Boolean = true,
    val canUsePromoUntil: Long? = null,
    val canOrder: Boolean = true,
    val suspensionReason: String = "",
    val suspensionUntil: Long? = null,
    val isArchived: Boolean = false,
    val permissions: String = "ALL", // Comma-separated permissions for fallback
    val isLoyaltyEnabled: Boolean = true,
    val loyaltyValidUntil: Long? = null,
    
    // --- NOUVEAU MOTEUR DE FIDÉLITÉ DYNAMIQUE CLIENT ---
    val accumulatedFreeKgs: Double = 0.0,
    val currentKgTowardsReward: Double = 0.0,
    val lifetimeKgWashed: Double = 0.0,
    
    // Anti-fraud personnalisable par client
    val customAutoBanEnabled: Boolean = false,
    val customAutoBanThreshold: Int = 0,
    val customAutoBanDurationHours: Int = 0,
    
    // Contrôles de création de commande
    val canSelectPieces: Boolean = true,
    val canSelectCategories: Boolean = true,
    val canEstimateKg: Boolean = true,
    val canUseExpress: Boolean = true,
    val canUsePremium: Boolean = true,

    // --- NOUVEAU SCHÉMA: BLOC StaffAdmins -> CreateNewAdmin ---
    val prenomAdmin: String = "",
    val nomAdmin: String = "",
    val mailAdmin: String = "",
    val motDePasseAdmin: String = "",
    val roleAdminSuper: Boolean = false, // true = SuperAdmin, false = Admin

    // --- NOUVEAU SCHÉMA: BLOC PermissionAdmin (Permissions Granulaires) ---
    val permVoirCommandes: Boolean = true,
    val permGererCommandes: Boolean = true,
    val permModifierHeureRamassage: Boolean = true,
    val permVoirClients: Boolean = true,
    val permGererClients: Boolean = true,
    val permSuspendreClients: Boolean = true,
    val permVoirLog: Boolean = true,
    val permGererPromosClient: Boolean = true,
    val permVoirCorbeille: Boolean = true,
    val permRestaurerCompte: Boolean = true,
    val permSupprimerDefinitivement: Boolean = true,
    val permVoirStocks: Boolean = true,
    val permGererStocks: Boolean = true,
    val permGererStocksVisibilite: Boolean = true,
    val permVoirCatalogue: Boolean = true,
    val permGererCatalogue: Boolean = true,
    val permGererPromos: Boolean = true,
    val permGererPromosAvancees: Boolean = true,
    val permGererGommage: Boolean = true,
    val permVoirParametres: Boolean = true,
    val permGererParametres: Boolean = true,
    val permVoirAntiFraudPersonnel: Boolean = true,
    val permGererAntiFraudPersonnel: Boolean = true,
    val permVoirAntiFraudGlobal: Boolean = true,
    val permGererAntiFraudGlobal: Boolean = true,

    // --- NOUVEAU SCHÉMA: BLOC ArchiveAdmins ---
    val suppressionDefinitiveAdmin: Boolean = false,
    val restaurerAdmin: Boolean = false,

    // --- NOUVEAU SCHÉMA: BLOC AccountUser -> EditProfilUser ---
    val prenomUser: String = "",
    val nomUser: String = "",
    val numeroUser: String = "",
    val mailUser: String = "",
    val adresseUser: String = "",
    val motDePasseUser: String = "",
    val cumulKiloFideliterUser: Double = 0.0,

    // --- NOUVEAU SCHÉMA: BLOC AccountUser -> EditAccesOrdersUser ---
    val autoriseCreatOrder: Boolean = true,
    val autoriseSaisiNombrePiece: Boolean = true,
    val autoriseSelectionCategory: Boolean = true,
    val autoriseSelectionCatalogue: Boolean = true,
    val autoriseSelectionArticlesSpecial: Boolean = true,
    val autoriseSelectionArticlesGommage: Boolean = true,
    val autoriseServiceLavage: Boolean = true,
    val autoriseLavageSimple: Boolean = true,
    val autoriseLavageSechagePliage: Boolean = true,
    val autoriseLavageSechagePliageRepassage: Boolean = true,
    val autoriseRamassageLinges: Boolean = true,
    val autoriseFixeeDateHeureRamassage: Boolean = true,
    val autoriseDateHeureLivraison: Boolean = true,
    val autoriseInstructionSpecial: Boolean = true,
    val autoriseServiceExpress: Boolean = true,
    val autoriseGarantiVIP: Boolean = true,
    val autoriseUtiliserCodePromos: Boolean = true,
    val autoriseVoirEstimationPrixFinal: Boolean = true,

    // --- NOUVEAU SCHÉMA: BLOC AccountUser -> ConfigAntiFraudUser ---
    val utiliseAntiFraudeGlobale: Boolean = true,

    // --- NOUVEAU SCHÉMA: BLOC AccountUser -> BloqPromoUser ---
    val blocagePersistant: Boolean = false,
    val blocageLimitee: Boolean = false,
    val dureeBlocage: Double = 0.0,

    // --- NOUVEAU SCHÉMA: BLOC AccountUser -> SuspendUser & BanUser ---
    val isBanned: Boolean = false,
    val estBanni: Boolean = false,
    val banReason: String = "",
    val raisonBannissement: String = "",
    val isBanPermanent: Boolean = false,
    val bannissementPermanent: Boolean = false,
    val banUntil: Long? = null,
    val dateFinBannissement: Long? = null,
    val estSuspendu: Boolean = false,
    val raisonSuspension: String = ""
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = "",
    val pieces: Int = 0,
    val categorie: String = "",
    val kg: Float = 0f,
    val lavageType: String = "SIMPLE",
    val gommagePaire: Int = 0,
    val gommageImpair: Int = 0,
    val prixFinal: Float = 0f,
    val status: String = "EN_ATTENTE", // "EN_ATTENTE", "PROPOSITION_ADMIN", "ACCEPTE", "TERMINE", "REJETE"
    val createdAt: Long = System.currentTimeMillis(),
    val adminMessage: String = "",
    val rating: Int = 0,
    val clientComment: String = "",
    val adresseLivraison: String = "",
    val isExpress: Boolean = false,
    val instructionsSpeciales: String = "",
    val deliveryType: String = "AGENCE", // "AGENCE" ou "DOMICILE"
    val transportFee: Float = 0f,
    val timelineStep: Int = 0, // 0: Envoyée, 1: Pesée/Acceptée, 2: Lavage, 3: Séchage/Repassage, 4: Prête, 5: Terminée
    val specialItemsData: String = "",
    val pickupDate: String = "",
    val deliveryDate: String = "",
    val photoUri: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceKm: Float = 0f,
    val promoCode: String = "",
    val discountPercentage: Float = 0f,
    val discountFixed: Float = 0f,
    val hasPremiumGuarantee: Boolean = false,
    val insuranceFee: Float = 0f,
    val isArchived: Boolean = false,
    val usedFreeKgs: Double = 0.0,
    val isLoyaltyPointsGranted: Boolean = false
)


@Keep
@IgnoreExtraProperties
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey val id: String = "global_config",
    val baseTransportFee: Float = 1000f,
    val expressMultiplier: Float = 1.3f,
    val fidelityThresholdKg: Float = 10f,
    val fidelityRewardValue: Float = 350f,
    val pricePerKm: Float = 500f,
    val pressingLat: Double = 12.6392,
    val pressingLng: Double = -8.0029,
    val referralRewardKg: Float = 2f,
    val appDownloadLink: String = "",
    val autoBanEnabled: Boolean = false,
    val autoBanThreshold: Int = 3,
    val autoBanDurationHours: Int = 24,
    val isLoyaltyEnabled: Boolean = true,
    val loyaltyValidUntil: Long? = null,
    
    // --- MODE MAINTENANCE ET SECOURS ---
    val isMaintenance: Boolean = false,
    val acceptNewOrders: Boolean = true,

    val categories: String = "Linge Mélangé,Costumes,Robes de Soirée,Chemises,Linge de Maison,Vestes,Pantalons",
    val businessName: String = "Bella Pressing",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val currencySymbol: String = "FCFA",
    val taxPercentage: Float = 0f,
    val openingHours: String = "",
    val closingHours: String = "",
    val openingDays: String = "Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi",
    val closingDays: String = "Dimanche",
    val termsAndConditionsLink: String = "",

    // --- NOUVEAU SCHÉMA: BLOC ConfigSeting ---
    val tarifBaseTransort: Double = 1000.0,
    val prixParKilo: Double = 350.0,

    // --- NOUVEAU SCHÉMA: BLOC InfosEntreprise ---
    val nomEntreprise: String = "BELLA — PRESSING",
    val numeroEntreprise: String = "",
    val emailEntreprise: String = "",
    val openDays: String = "Lundi,Mardi,Mercredi,Jeudi,Vendredi,Samedi",
    val openHours: String = "",
    val closeHours: String = "",
    val linkCGU: String = "",

    // --- PROGRAMME DE FIDÉLITÉ DYNAMIQUE: KILOS GRATUITS & EXPIRATION ---
    val isLoyaltyProgramEnabled: Boolean = true,
    val loyaltyKgThreshold: Double = 10.0,
    val loyaltyKgReward: Double = 1.0,
    val loyaltyMaxFreeKgs: Double = 30.0,
    val loyaltyExpirationDays: Int = 90
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "anti_fraud_rules")
data class AntiFraudRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val triggerEvent: String = "",
    val threshold: Int = 0,
    val durationHours: Int = 0,
    val action: String = "BAN",
    val isActive: Boolean = true,
    val isArchived: Boolean = false,

    // --- NOUVEAU SCHÉMA: BLOC AntiFraudGlobal ---
    val nomNewAntiFraud: String = "",
    val evenAntiFraud: String = "",
    val seuilAntiFraud: Double = 0.0,
    val dureeAntiFraud: Double = 0.0,
    val actionAntiFraud: String = ""
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "special_items")
data class SpecialItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val price: Float = 0f,
    val isArchived: Boolean = false
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "gommage_items")
data class GommageItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val price: Float = 0f,
    val isArchived: Boolean = false
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "consumables")
data class ConsumableEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: Int = 0,
    val threshold: Int = 0,
    val isPublic: Boolean = false,
    val isArchived: Boolean = false,

    // --- NOUVEAU SCHÉMA: BLOC Stocks -> NewStocks ---
    val nomProduit: String = "",
    val quantiterProduit: Double = 0.0,
    val produitSeilleAlert: Double = 0.0,
    val newStocksPublic: Boolean = false,
    val newStocksPrivee: Boolean = true
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "promos")
data class PromoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val code: String = "",
    val description: String = "",
    val discountPercent: Float = 0f,
    val freeKg: Float = 0f,
    val minOrderValue: Float = 0f,
    val isActive: Boolean = true,
    val validUntil: Long? = null,
    val isPermanent: Boolean = true,
    val targetItems: String = "ALL",
    val discountType: String = "PERCENTAGE",
    val isArchived: Boolean = false
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val triggerCondition: String = "",
    val actionType: String = "",
    val actionValue: String = "",
    val constraints: String = "",
    val isActive: Boolean = true,
    val isPermanent: Boolean = true,
    val validUntil: Long? = null,
    val isArchived: Boolean = false
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val orderId: String? = null,
    val action: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val actorId: String? = null
)

@Keep
@IgnoreExtraProperties
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val isVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),

    // --- NOUVEAU SCHÉMA: BLOC Annonce&PublicationAdminForUser -> CreatNewAnnoceAdmin ---
    val nomAnnoce: String = "",
    val descriptionAnnoce: String = "",
    val fichiersMediaAnnoce: String = ""
)

// --- NOUVEAU SCHÉMA: BLOC CategiriesLingeSaved ---
@Keep
@IgnoreExtraProperties
@Entity(tableName = "categories_linge_saved")
data class CategoriesLingeSavedEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val newCategorieLinge: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// --- NOUVEAU SCHÉMA: BLOC TableauDeBord ---
@Keep
@IgnoreExtraProperties
@Entity(tableName = "tableau_de_bord")
data class TableauDeBordEntity(
    @PrimaryKey val id: String = "global_dashboard",
    val enAttend: Double = 0.0,
    val acceptee: Double = 0.0,
    val rejeter: Double = 0.0,
    val termine: Double = 0.0,
    val total: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

// --- NOUVEAU SCHÉMA: BLOC OrdersHistory ---
@Keep
@IgnoreExtraProperties
@Entity(tableName = "orders_history")
data class OrdersHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val allHistory: String = "",
    val attendHistory: String = "",
    val accepteeHistory: String = "",
    val terminerHistory: String = "",
    val rejeterHistory: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

// --- NOUVEAU SCHÉMA: BLOC ArchivesGlobals ---
@Keep
@IgnoreExtraProperties
@Entity(tableName = "global_trash")
data class GlobalTrashEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val originalId: String = "",
    val itemType: String = "", // "USER", "ORDER", "CATALOG_SPECIAL", "CATALOG_GOMMAGE", "CONSUMABLE", "PROMO", "RULE", "ANTI_FRAUD"
    val title: String = "",
    val details: String = "",
    val deletedAt: Long = System.currentTimeMillis(),
    val adminResponsible: String = "",
    val serializedData: String = ""
)
