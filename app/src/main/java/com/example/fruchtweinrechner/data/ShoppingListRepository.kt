package com.example.fruchtweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ShoppingListRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("shoppingStatus")

    val allStatus: Flow<Map<String, ShoppingListStatus>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyMap())
                return@addSnapshotListener
            }
            val map = snapshot?.documents?.associate { doc ->
                doc.id to (doc.toObject(ShoppingListStatus::class.java) ?: ShoppingListStatus())
            } ?: emptyMap()
            trySend(map)
        }
        awaitClose { listener.remove() }
    }

    suspend fun setStatus(itemId: String, status: ShoppingListStatus) {
        collection.document(itemId).set(status).await()
    }
}
