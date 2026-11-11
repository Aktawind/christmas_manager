package com.aktawind.monnoel.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun UiMessage.text(): String = stringResource(resId, *args.toTypedArray())

fun UiMessage.resolve(context: android.content.Context): String = context.getString(resId, *args.toTypedArray())
