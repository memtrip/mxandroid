/**
 * Copyright 2013-present memtrip LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.memtrip.mxandroid

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

import io.reactivex.disposables.CompositeDisposable

abstract class MxViewFragment<VI : MxViewIntent, RA : MxRenderAction, VS : MxViewState, VL : MxViewLayout> : Fragment() {

    private val d = CompositeDisposable()

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        inject()
    }

    override fun onStart() {
        super.onStart()

        d.add(
                model().states()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { state -> render().layout(layout(), state) }
        )

        model().processIntents(intents())
    }

    override fun onStop() {
        super.onStop()
        d.clear()
    }

    abstract fun intents(): Observable<VI>

    abstract fun inject()

    abstract fun layout(): VL

    abstract fun model(): MxViewModel<VI, RA, VS>

    abstract fun render(): MxViewRenderer<VL, VS>
}