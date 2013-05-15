/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.noser.neo4j.android.dbinspector.implementation;

import java.util.Map;

import roboguice.RoboGuice;
import roboguice.util.Ln;
import android.app.Application;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class DBInspectorApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Module defaultModule = RoboGuice.newDefaultRoboModule(this);
        Module dbInspectorModule = new DBInspectorModule();
        Module combinedModule = Modules.combine(defaultModule, dbInspectorModule);

        Injector injector = RoboGuice.setBaseApplicationInjector(this, RoboGuice.DEFAULT_STAGE, combinedModule);

        Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();
        for (Key<?> key : bindings.keySet()) {
            Binding<?> value = bindings.get(key);
            Ln.d("binding key '" + key + "', value '" + value + "'");
        }

        Ln.i("Application initialized.");
    }
}
