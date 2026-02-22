package com.example.loveapp

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SimpleTestViewModel @Inject constructor() : ViewModel() {
    init {
        Log.d("LoveApp", "SimpleTestViewModel created")
    }
}