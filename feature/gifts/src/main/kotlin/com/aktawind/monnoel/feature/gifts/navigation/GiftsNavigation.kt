package com.aktawind.monnoel.feature.gifts.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aktawind.monnoel.feature.gifts.GiftsRoute
import com.aktawind.monnoel.feature.gifts.edit.GiftEditRoute
import com.aktawind.monnoel.feature.gifts.edit.IdeaEditRoute
import com.aktawind.monnoel.feature.gifts.person.PersonEditRoute
import com.aktawind.monnoel.feature.gifts.person.PersonRoute
import kotlinx.serialization.Serializable

@Serializable
data object GiftsDestination

@Serializable
data class GiftEditDestination(val giftId: String? = null, val personId: String? = null)

@Serializable
data class IdeaEditDestination(val ideaId: String? = null, val personId: String? = null)

@Serializable
data class PersonDestination(val personId: String)

@Serializable
data class PersonEditDestination(val personId: String? = null)

fun NavGraphBuilder.giftsGraph(navController: NavController) {
    composable<GiftsDestination> {
        GiftsRoute(
            onOpenGift = { navController.navigate(GiftEditDestination(giftId = it)) },
            onNewGift = { navController.navigate(GiftEditDestination()) },
            onOpenPerson = { navController.navigate(PersonDestination(it)) },
            onNewPerson = { navController.navigate(PersonEditDestination()) },
            onOpenIdea = { navController.navigate(IdeaEditDestination(ideaId = it)) },
            onNewIdea = { navController.navigate(IdeaEditDestination()) },
        )
    }
    composable<GiftEditDestination> { GiftEditRoute(onDone = { navController.popBackStack() }) }
    composable<IdeaEditDestination> { IdeaEditRoute(onDone = { navController.popBackStack() }) }
    composable<PersonDestination> {
        PersonRoute(
            onBack = { navController.popBackStack() },
            onEdit = { navController.navigate(PersonEditDestination(it)) },
            onOpenGift = { navController.navigate(GiftEditDestination(giftId = it)) },
            onNewGift = { navController.navigate(GiftEditDestination(personId = it)) },
            onOpenIdea = { navController.navigate(IdeaEditDestination(ideaId = it)) },
            onNewIdea = { navController.navigate(IdeaEditDestination(personId = it)) },
        )
    }
    composable<PersonEditDestination> {
        PersonEditRoute(
            onSaved = { id, isNew ->
                navController.popBackStack()
                if (isNew) navController.navigate(PersonDestination(id))
            },
            onDeleted = { navController.popBackStack(GiftsDestination, inclusive = false) },
            onCancel = { navController.popBackStack() },
        )
    }
}
