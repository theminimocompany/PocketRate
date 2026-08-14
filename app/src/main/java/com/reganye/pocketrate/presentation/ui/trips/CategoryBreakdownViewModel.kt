package com.reganye.pocketrate.presentation.ui.trips

import androidx.lifecycle.ViewModel
import com.reganye.pocketrate.domain.usecase.GetCategoryBreakdownUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CategoryBreakdownViewModel @Inject constructor(
    val useCase: GetCategoryBreakdownUseCase
) : ViewModel()
