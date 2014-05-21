package akka.http.model.japi.headers;

import akka.japi.Option;

import java.net.InetAddress;

public abstract class RemoteAddress {
    public abstract boolean isUnknown();

    public abstract Option<InetAddress> getAddress();

    public static final RemoteAddress Unknown = akka.http.model.headers.RemoteAddress.Unknown$.MODULE$;
    public static RemoteAddress create(InetAddress address) {
        return akka.http.model.headers.RemoteAddress.apply(address);
    }
    public static RemoteAddress create(String address) {
        return akka.http.model.headers.RemoteAddress.apply(address);
    }
    public static RemoteAddress create(byte[] address) {
        return akka.http.model.headers.RemoteAddress.apply(address);
    }
}
