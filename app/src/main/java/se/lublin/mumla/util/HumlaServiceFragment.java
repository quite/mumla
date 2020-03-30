/*
 * Copyright (C) 2014 Andrew Comminos
 *
 * This program is free software: you can redistribute it and/or modify
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

package se.lublin.mumla.util;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import se.lublin.humla.IHumlaService;
import se.lublin.humla.util.IHumlaObserver;
import se.lublin.mumla.service.IMumlaService;

/**
 * Fragment class intended to make binding the Humla service to fragments easier.
 * Created by andrew on 04/08/13.
 */
public abstract class HumlaServiceFragment extends Fragment {

    private HumlaServiceProvider mServiceProvider;

    /** State boolean to make sure we don't double initialize a fragment once a service has been bound. */
    private boolean mBound;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mServiceProvider = (HumlaServiceProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement HumlaServiceProvider");
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mServiceProvider.addServiceFragment(this);
        if(mServiceProvider.getService() != null && !mBound)
            onServiceAttached(mServiceProvider.getService());
    }

    @Override
    public void onDestroy() {
        mServiceProvider.removeServiceFragment(this);
        if(mServiceProvider.getService() != null && mBound)
            onServiceDetached(mServiceProvider.getService());
        super.onDestroy();
    }

    /** The definitive place where data from the service will be used to initialize the fragment. Only called once per bind, whether the fragment loads first or the service. */
    public void onServiceBound(IHumlaService service) { }

    public void onServiceUnbound() { }

    /** If implemented, will register the returned observer to the service upon binding. */
    public IHumlaObserver getServiceObserver() {
        return null;
    }

    private void onServiceAttached(IHumlaService service) {
        mBound = true;
        if(getServiceObserver() != null)
            service.registerObserver(getServiceObserver());

        onServiceBound(service);
    }

    private void onServiceDetached(IHumlaService service) {
        mBound = false;
        if(getServiceObserver() != null)
            service.unregisterObserver(getServiceObserver());

        onServiceUnbound();
    }

    public void setServiceBound(boolean bound) {
        if(bound && !mBound)
            onServiceAttached(mServiceProvider.getService());
        else if(mBound && !bound)
            onServiceDetached(mServiceProvider.getService());
    }

    public IMumlaService getService() {
        return mServiceProvider.getService();
    }
}
