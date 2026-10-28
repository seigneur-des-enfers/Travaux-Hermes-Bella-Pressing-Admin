package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object dédié aux commandes Admin et Livreur.
 * Architecture Offline-First avec Room comme Single Source of Truth (SSOT).
 */
@Dao
interface OrderDao {

    /**
     * Flux réactif observant en temps réel les commandes actives
     * (Statuts : EN_ATTENTE, EN_COURS, PRET, ACCEPTE, EN_LIVRAISON).
     */
    @Query("""
        SELECT * FROM orders 
        WHERE status IN ('EN_ATTENTE', 'PROPOSITION_ADMIN', 'EN_COURS', 'PRET', 'ACCEPTE', 'EN_LIVRAISON')
          AND isArchived = 0 
        ORDER BY createdAt DESC
    """)
    fun getActiveOrders(): Flow<List<OrderEntity>>

    /**
     * Flux réactif observant l'historique des commandes clôturées
     * (Statuts : LIVRE, TERMINE, ANNULE, REJETE ou archivées).
     */
    @Query("""
        SELECT * FROM orders 
        WHERE status IN ('LIVRE', 'TERMINE', 'ANNULE', 'REJETE') 
           OR isArchived = 1 
        ORDER BY createdAt DESC
    """)
    fun getHistoricalOrders(): Flow<List<OrderEntity>>

    /**
     * Récupère toutes les commandes actives et non archivées (fallback / administration globale).
     */
    @Query("SELECT * FROM orders WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    /**
     * Récupère les commandes d'un utilisateur spécifique.
     */
    @Query("SELECT * FROM orders WHERE userId = :userId AND isArchived = 0 ORDER BY createdAt DESC")
    fun getOrdersForUser(userId: String): Flow<List<OrderEntity>>

    /**
     * Observe une commande unique par son identifiant.
     */
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?>

    /**
     * Récupère une commande unique par son identifiant.
     */
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    /**
     * Insertion unitaire avec stratégie de remplacement immédiat (SSOT).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    /**
     * Insertion par lot avec stratégie de remplacement immédiat (Batch SSOT).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    /**
     * Mise à jour d'une commande.
     */
    @Update
    suspend fun updateOrder(order: OrderEntity)

    /**
     * Suppression explicite d'une commande par son ID.
     */
    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: String)

    /**
     * SÉCURITÉ ET PROTECTION DES DONNÉES :
     * Permet de vider l'intégralité du cache local à la déconnexion de l'administrateur ou du livreur.
     */
    @Query("DELETE FROM orders")
    suspend fun clearAllOrders()

    /**
     * Archivage logique d'une commande.
     */
    @Query("UPDATE orders SET isArchived = 1 WHERE id = :orderId")
    suspend fun archiveOrder(orderId: String)

    /**
     * Restauration d'une commande archivée.
     */
    @Query("UPDATE orders SET isArchived = 0 WHERE id = :orderId")
    suspend fun restoreOrder(orderId: String)

    /**
     * Supprime toutes les commandes clôturées/historiques pour préparer une resynchronisation propre sans données fantômes.
     */
    @Query("DELETE FROM orders WHERE status IN ('LIVRE', 'TERMINE', 'ANNULE', 'REJETE') OR isArchived = 1")
    suspend fun deleteAllOrdersHistory()

    /**
     * Synchronisation atomique de l'historique : purge locale + insertion en une transaction unique.
     */
    @androidx.room.Transaction
    suspend fun syncHistoricalOrders(orders: List<OrderEntity>) {
        deleteAllOrdersHistory()
        insertOrders(orders)
    }
}
