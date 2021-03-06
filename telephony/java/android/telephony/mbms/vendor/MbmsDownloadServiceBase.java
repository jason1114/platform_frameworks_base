/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.telephony.mbms.vendor;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.telephony.mbms.DownloadStateCallback;
import android.telephony.mbms.DownloadRequest;
import android.telephony.mbms.FileInfo;
import android.telephony.mbms.FileServiceInfo;
import android.telephony.mbms.IDownloadStateCallback;
import android.telephony.mbms.IMbmsDownloadManagerCallback;
import android.telephony.mbms.MbmsDownloadManagerCallback;
import android.telephony.mbms.MbmsException;

import java.util.List;

/**
 * Base class for MbmsDownloadService. The middleware should return an instance of this object from
 * its {@link android.app.Service#onBind(Intent)} method.
 * @hide
 */
@SystemApi
public class MbmsDownloadServiceBase extends IMbmsDownloadService.Stub {
    /**
     * Initialize the download service for this app and subId, registering the listener.
     *
     * May throw an {@link IllegalArgumentException} or an {@link IllegalStateException}, which
     * will be intercepted and passed to the app as
     * {@link android.telephony.mbms.MbmsException.InitializationErrors#ERROR_UNABLE_TO_INITIALIZE}
     *
     * May return any value from {@link android.telephony.mbms.MbmsException.InitializationErrors}
     * or {@link MbmsException#SUCCESS}. Non-successful error codes will be passed to the app via
     * {@link IMbmsDownloadManagerCallback#error(int, String)}.
     *
     * @param callback The callback to use to communicate with the app.
     * @param subscriptionId The subscription ID to use.
     */
    public int initialize(int subscriptionId, MbmsDownloadManagerCallback callback)
            throws RemoteException {
        return 0;
    }

    /**
     * Actual AIDL implementation -- hides the callback AIDL from the API.
     * @hide
     */
    @Override
    public final int initialize(final int subscriptionId,
            final IMbmsDownloadManagerCallback callback) throws RemoteException {
        final int uid = Binder.getCallingUid();
        callback.asBinder().linkToDeath(new DeathRecipient() {
            @Override
            public void binderDied() {
                onAppCallbackDied(uid, subscriptionId);
            }
        }, 0);

        return initialize(subscriptionId, new MbmsDownloadManagerCallback() {
            @Override
            public void onError(int errorCode, String message) {
                try {
                    callback.error(errorCode, message);
                } catch (RemoteException e) {
                    onAppCallbackDied(uid, subscriptionId);
                }
            }

            @Override
            public void onFileServicesUpdated(List<FileServiceInfo> services) {
                try {
                    callback.fileServicesUpdated(services);
                } catch (RemoteException e) {
                    onAppCallbackDied(uid, subscriptionId);
                }
            }

            @Override
            public void onMiddlewareReady() {
                try {
                    callback.middlewareReady();
                } catch (RemoteException e) {
                    onAppCallbackDied(uid, subscriptionId);
                }
            }
        });
    }

    /**
     * Registers serviceClasses of interest with the appName/subId key.
     * Starts async fetching data on streaming services of matching classes to be reported
     * later via {@link IMbmsDownloadManagerCallback#fileServicesUpdated(List)}
     *
     * Note that subsequent calls with the same uid and subId will replace
     * the service class list.
     *
     * May throw an {@link IllegalArgumentException} or an {@link IllegalStateException}
     *
     * @param subscriptionId The subscription id to use.
     * @param serviceClasses The service classes that the app wishes to get info on. The strings
     *                       may contain arbitrary data as negotiated between the app and the
     *                       carrier.
     * @return One of {@link MbmsException#SUCCESS} or
     *         {@link MbmsException.GeneralErrors#ERROR_MIDDLEWARE_NOT_YET_READY},
     */
    @Override
    public int getFileServices(int subscriptionId, List<String> serviceClasses)
            throws RemoteException {
        return 0;
    }

    /**
     * Sets the temp file root directory for this app/subscriptionId combination. The middleware
     * should persist {@code rootDirectoryPath} and send it back when sending intents to the
     * app's {@link android.telephony.mbms.MbmsDownloadReceiver}.
     *
     * If the calling app (as identified by the calling UID) currently has any pending download
     * requests that have not been canceled, the middleware must return
     * {@link MbmsException.DownloadErrors#ERROR_CANNOT_CHANGE_TEMP_FILE_ROOT} here.
     *
     * @param subscriptionId The subscription id the download is operating under.
     * @param rootDirectoryPath The path to the app's temp file root directory.
     * @return {@link MbmsException#SUCCESS},
     *         {@link MbmsException.GeneralErrors#ERROR_MIDDLEWARE_NOT_YET_READY} or
     *         {@link MbmsException.DownloadErrors#ERROR_CANNOT_CHANGE_TEMP_FILE_ROOT}
     */
    @Override
    public int setTempFileRootDirectory(int subscriptionId,
            String rootDirectoryPath) throws RemoteException {
        return 0;
    }

    /**
     * Issues a request to download a set of files.
     *
     * The middleware should expect that {@link #setTempFileRootDirectory(int, String)} has been
     * called for this app between when the app was installed and when this method is called. If
     * this is not the case, an {@link IllegalStateException} may be thrown.
     *
     * @param downloadRequest An object describing the set of files to be downloaded.
     * @param callback A callback through which the middleware can provide progress updates to
     *                 the app while both are still running.
     * @return Any error from {@link android.telephony.mbms.MbmsException.GeneralErrors}
     *         or {@link MbmsException#SUCCESS}
     */
    public int download(DownloadRequest downloadRequest, DownloadStateCallback callback) {
        return 0;
    }

    /**
     * Actual AIDL implementation -- hides the callback AIDL from the API.
     * @hide
     */
    @Override
    public final int download(DownloadRequest downloadRequest, IDownloadStateCallback callback)
            throws RemoteException {
        final int uid = Binder.getCallingUid();
        callback.asBinder().linkToDeath(new DeathRecipient() {
            @Override
            public void binderDied() {
                onAppCallbackDied(uid, downloadRequest.getSubscriptionId());
            }
        }, 0);

        return download(downloadRequest, new DownloadStateCallback() {
            @Override
            public void onProgressUpdated(DownloadRequest request, FileInfo fileInfo, int
                    currentDownloadSize, int fullDownloadSize, int currentDecodedSize, int
                    fullDecodedSize) {
                try {
                    callback.progress(request, fileInfo, currentDownloadSize, fullDownloadSize,
                            currentDecodedSize, fullDecodedSize);
                } catch (RemoteException e) {
                    onAppCallbackDied(uid, downloadRequest.getSubscriptionId());
                }
            }
        });
    }


    /**
     * Returns a list of pending {@link DownloadRequest}s that originated from the calling
     * application, identified by its uid. A pending request is one that was issued via
     * {@link #download(DownloadRequest, DownloadStateCallback)} but not cancelled through
     * {@link #cancelDownload(DownloadRequest)}.
     * The middleware must return a non-null result synchronously or throw an exception
     * inheriting from {@link RuntimeException}.
     * @return A list, possibly empty, of {@link DownloadRequest}s
     */
    @Override
    public @NonNull List<DownloadRequest> listPendingDownloads(int subscriptionId)
            throws RemoteException {
        return null;
    }

    /**
     * Issues a request to cancel the specified download request.
     *
     * If the middleware is unable to cancel the request for whatever reason, it should return
     * synchronously with an error. If this method returns {@link MbmsException#SUCCESS}, the app
     * will no longer be expecting any more file-completed intents from the middleware for this
     * {@link DownloadRequest}.
     * @param downloadRequest The request to cancel
     * @return {@link MbmsException#SUCCESS},
     *         {@link MbmsException.DownloadErrors#ERROR_UNKNOWN_DOWNLOAD_REQUEST},
     *         {@link MbmsException.GeneralErrors#ERROR_MIDDLEWARE_NOT_YET_READY}
     */
    @Override
    public int cancelDownload(DownloadRequest downloadRequest) throws RemoteException {
        return 0;
    }

    /**
     * Gets information about the status of a file pending download.
     *
     * If the middleware has not yet been properly initialized or if it has no records of the
     * file indicated by {@code fileInfo} being associated with {@code downloadRequest},
     * {@link android.telephony.MbmsDownloadManager#STATUS_UNKNOWN} must be returned.
     *
     * @param downloadRequest The download request to query.
     * @param fileInfo The particular file within the request to get information on.
     * @return The status of the download.
     */
    @Override
    public int getDownloadStatus(DownloadRequest downloadRequest, FileInfo fileInfo)
            throws RemoteException {
        return 0;
    }

    /**
     * Resets the middleware's knowledge of previously-downloaded files in this download request.
     *
     * When this method is called, the middleware must attempt to re-download all the files
     * specified by the {@link DownloadRequest}, even if the files have not changed on the server.
     * In addition, current in-progress downloads must not be interrupted.
     *
     * If the middleware is not aware of the specified download request, return
     * {@link MbmsException.DownloadErrors#ERROR_UNKNOWN_DOWNLOAD_REQUEST}.
     *
     * @param downloadRequest The request to re-download files for.
     */
    @Override
    public int resetDownloadKnowledge(DownloadRequest downloadRequest)
            throws RemoteException {
        return 0;
    }

    /**
     * Signals that the app wishes to dispose of the session identified by the
     * {@code subscriptionId} argument and the caller's uid. No notification back to the
     * app is required for this operation, and the corresponding callback provided via
     * {@link #initialize(int, IMbmsDownloadManagerCallback)} should no longer be used
     * after this method has been called by the app.
     *
     * Any download requests issued by the app should remain in effect until the app calls
     * {@link #cancelDownload(DownloadRequest)} on another session.
     *
     * May throw an {@link IllegalStateException}
     *
     * @param subscriptionId The subscription id to use.
     */
    @Override
    public void dispose(int subscriptionId) throws RemoteException {
    }

    /**
     * Indicates that the app identified by the given UID and subscription ID has died.
     * @param uid the UID of the app, as returned by {@link Binder#getCallingUid()}.
     * @param subscriptionId The subscription ID the app is using.
     */
    public void onAppCallbackDied(int uid, int subscriptionId) {
    }
}
