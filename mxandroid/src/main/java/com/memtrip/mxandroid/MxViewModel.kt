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

import android.app.Application

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PACKAGE_PRIVATE
import androidx.lifecycle.AndroidViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

abstract class MxViewModel<VI : MxViewIntent, RA : MxRenderAction, VS : MxViewState>(
    initialState: VS,
    application: Application
) : AndroidViewModel(application) {

    private val intentsSubject: PublishSubject<VI> = PublishSubject.create()
    private val statesObservable = intentsSubject
            .compose { intents ->
                intents.publish {
                    filterIntents(intents)
                }
            }
            .flatMap {
                intercept(it)
                dispatcher(it)
            }
            .scan(initialState) { previousState: VS, renderAction: RA ->
                intercept(renderAction)
                reducer(previousState, renderAction)
            }.map {
                intercept(it)
                it
            }
            .distinctUntilChanged()
            .replay(1)
            .autoConnect(0)

    fun publish(intent: VI) {
        intentsSubject.onNext(intent)
    }

    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    fun processIntents(intents: Observable<VI>) {
        intents.subscribe(intentsSubject)
    }

    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    fun states(): Observable<VS> = statesObservable

    abstract fun reducer(previousState: VS, renderAction: RA): VS

    abstract fun dispatcher(intent: VI): Observable<RA>

    open fun filterIntents(intents: Observable<VI>): Observable<VI> = intents

    protected fun <T> observable(item: T): Observable<T> = Observable.just(item)

    protected fun context(): Context = getApplication()

    /**
     * Provide a closure that gets called before each intent is processed
     */
    open fun intentInterceptor(): ((intent: VI) -> Unit)? = null

    private fun intercept(intent: VI) {
        if (intentInterceptor() != null) {
            intentInterceptor()?.invoke(intent)
        }
    }

    /**
     * Provide a closure that gets called before the render action is processed
     */
    open fun renderInterceptor(): ((renderAction: RA) -> Unit)? = null

    private fun intercept(renderAction: RA) {
        if (intentInterceptor() != null) {
            renderInterceptor()?.invoke(renderAction)
        }
    }

    /**
     * Provide a closure that gets called before each view state
     */
    open fun viewStateInterceptor(): ((viewState: VS) -> Unit)? = null

    private fun intercept(viewState: VS) {
        if (viewStateInterceptor() != null) {
            viewStateInterceptor()?.invoke(viewState)
        }
    }
}