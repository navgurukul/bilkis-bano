/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.terms

import im.vector.matrix.android.internal.network.HttpHeaders
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

internal interface TermsAPI {
    /**
     * This request does not require authentication
     */
    @GET
    fun getTerms(@Url url: String): Call<TermsResponse>

    /**
     * This request requires authentication
     */
    @POST
    fun agreeToTerms(@Url url: String, @Body params: AcceptTermsBody, @Header(HttpHeaders.Authorization) token: String): Call<Unit>
}
