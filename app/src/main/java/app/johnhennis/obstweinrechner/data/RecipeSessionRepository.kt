package app.johnhennis.obstweinrechner.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Bewusst einmaliges Nachladen (kein Live-Listener), damit beim Tippen keine
// Schreib-Lese-Rückkopplung entsteht. Wird beim Sortenwechsel neu geladen.
class RecipeSessionRepository(private val firestore: FirebaseFirestore) {

    private val collection = firestore.collection("recipeSessions")

    suspend fun getOnce(recipeId: String): RecipeSession {
        if (recipeId.isEmpty()) return RecipeSession()
        val doc = collection.document(recipeId).get().await()
        return doc.toObject(RecipeSession::class.java) ?: RecipeSession(recipeId = recipeId)
    }

    suspend fun save(session: RecipeSession) {
        if (session.recipeId.isEmpty()) return
        collection.document(session.recipeId).set(session).await()
    }
}
