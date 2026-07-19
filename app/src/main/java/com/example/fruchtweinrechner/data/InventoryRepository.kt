package com.example.fruchtweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class InventoryRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("inventory")

    val allItems: Flow<List<InventoryItem>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(InventoryItem::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            trySend(items.sortedBy { it.name })
        }
        awaitClose { listener.remove() }
    }

    suspend fun insert(item: InventoryItem) {
        collection.add(item.copy(id = "")).await()
    }

    suspend fun update(item: InventoryItem) {
        collection.document(item.id).set(item).await()
    }

    suspend fun delete(item: InventoryItem) {
        collection.document(item.id).delete().await()
    }
}
