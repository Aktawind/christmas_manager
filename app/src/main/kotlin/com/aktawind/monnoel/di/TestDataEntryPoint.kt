package com.aktawind.monnoel.di

import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.advent.SaveBoxUseCase
import com.aktawind.monnoel.core.domain.advent.SaveCalendarUseCase
import com.aktawind.monnoel.core.domain.advent.SaveSupplyUseCase
import com.aktawind.monnoel.core.domain.budget.SaveExpenseUseCase
import com.aktawind.monnoel.core.domain.budget.SetEditionBudgetUseCase
import com.aktawind.monnoel.core.domain.cooking.SaveProductionUseCase
import com.aktawind.monnoel.core.domain.cooking.SaveRecipeUseCase
import com.aktawind.monnoel.core.domain.countdown.SaveSpecialDateUseCase
import com.aktawind.monnoel.core.domain.edition.CreateEditionUseCase
import com.aktawind.monnoel.core.domain.edition.SelectEditionUseCase
import com.aktawind.monnoel.core.domain.gifts.ChangeGiftStatusUseCase
import com.aktawind.monnoel.core.domain.gifts.CreateIdeaRequestUseCase
import com.aktawind.monnoel.core.domain.gifts.OfferIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.SaveGiftUseCase
import com.aktawind.monnoel.core.domain.gifts.SaveIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.SavePersonUseCase
import com.aktawind.monnoel.core.domain.gifts.SaveWhisperUseCase
import com.aktawind.monnoel.core.domain.gifts.SetPersonBudgetUseCase
import com.aktawind.monnoel.core.domain.meals.SaveDishUseCase
import com.aktawind.monnoel.core.domain.meals.SaveMealUseCase
import com.aktawind.monnoel.core.domain.tasks.SaveTaskUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveCraftSupplyUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveCraftUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveDecorUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveInspirationUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveTraditionMemoryUseCase
import com.aktawind.monnoel.core.domain.traditions.SaveTraditionUseCase
import com.aktawind.monnoel.core.domain.traditions.SetCraftStatusUseCase
import com.aktawind.monnoel.core.domain.traditions.SetCraftSupplyBoughtUseCase
import com.aktawind.monnoel.core.domain.traditions.SetInspirationDoneUseCase
import com.aktawind.monnoel.core.domain.traditions.SetTraditionPlannedUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Accès aux cas d'usage pour préparer des données d'exemple dans les tests (captures d'écran). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestDataEntryPoint {
    fun createEdition(): CreateEditionUseCase

    fun savePerson(): SavePersonUseCase

    fun saveIdea(): SaveIdeaUseCase

    fun saveGift(): SaveGiftUseCase

    fun offerIdea(): OfferIdeaUseCase

    fun changeStatus(): ChangeGiftStatusUseCase

    fun setBudget(): SetPersonBudgetUseCase

    fun saveCalendar(): SaveCalendarUseCase

    fun saveBox(): SaveBoxUseCase

    fun saveSupply(): SaveSupplyUseCase

    fun adventRepository(): AdventRepository

    fun saveTask(): SaveTaskUseCase

    fun saveRecipe(): SaveRecipeUseCase

    fun saveProduction(): SaveProductionUseCase

    fun saveMeal(): SaveMealUseCase

    fun saveDish(): SaveDishUseCase

    fun saveExpense(): SaveExpenseUseCase

    fun setEditionBudget(): SetEditionBudgetUseCase

    fun selectEdition(): SelectEditionUseCase

    fun saveWhisper(): SaveWhisperUseCase

    fun createIdeaRequest(): CreateIdeaRequestUseCase

    fun saveSpecialDate(): SaveSpecialDateUseCase

    fun saveTradition(): SaveTraditionUseCase

    fun planTradition(): SetTraditionPlannedUseCase

    fun saveTraditionMemory(): SaveTraditionMemoryUseCase

    fun saveInspiration(): SaveInspirationUseCase

    fun setInspirationDone(): SetInspirationDoneUseCase

    fun saveCraft(): SaveCraftUseCase

    fun saveCraftSupply(): SaveCraftSupplyUseCase

    fun setCraftStatus(): SetCraftStatusUseCase

    fun setCraftSupplyBought(): SetCraftSupplyBoughtUseCase

    fun saveDecor(): SaveDecorUseCase
}
