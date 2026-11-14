package com.aktawind.monnoel.di

import com.aktawind.monnoel.core.domain.advent.AdventRepository
import com.aktawind.monnoel.core.domain.advent.SaveBoxUseCase
import com.aktawind.monnoel.core.domain.advent.SaveCalendarUseCase
import com.aktawind.monnoel.core.domain.advent.SaveSupplyUseCase
import com.aktawind.monnoel.core.domain.edition.CreateEditionUseCase
import com.aktawind.monnoel.core.domain.gifts.ChangeGiftStatusUseCase
import com.aktawind.monnoel.core.domain.gifts.OfferIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.SaveGiftUseCase
import com.aktawind.monnoel.core.domain.gifts.SaveIdeaUseCase
import com.aktawind.monnoel.core.domain.gifts.SavePersonUseCase
import com.aktawind.monnoel.core.domain.gifts.SetPersonBudgetUseCase
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
}
