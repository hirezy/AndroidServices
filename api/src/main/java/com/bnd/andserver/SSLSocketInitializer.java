
package com.bnd.andserver;

import androidx.annotation.NonNull;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
public interface SSLSocketInitializer {

    void onCreated(@NonNull SSLServerSocket socket) throws SSLException;
}