package com.company;

public enum HostEnum {

    BE1("192.168.0.167"),
    BE2("192.168.0.168"),
    BE3("192.168.0.171"),
    I1("192.168.7.235"),
    I2("192.168.7.236"),
    I3("192.168.7.237"),
    IA1("192.168.20.14");

    private String ip;

    HostEnum(String ip) {

        this.ip = ip;
    }

    public String getIp() {
        return this.ip;
    }
}
