/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/

package org.opendcs.ratings.io.jdbc;


import org.opendcs.ratings.io.RatingConnectionInfo;
import org.opendcs.ratings.RatingException;

final class JdbcRatingConnectionInfo implements RatingConnectionInfo {

    private final ConnectionProvider connectionProvider;

    JdbcRatingConnectionInfo(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public <T> T getConnectionInfo(Class<T> type) throws RatingException {
        if(type.isAssignableFrom(ConnectionProvider.class)) {
            return (T) connectionProvider;
        } else {
            throw new RatingException("getClass() does not support connection provider type: " + type);
        }
    }
}
