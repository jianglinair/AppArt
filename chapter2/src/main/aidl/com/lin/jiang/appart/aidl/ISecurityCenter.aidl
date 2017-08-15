package com.lin.jiang.appart.aidl;

interface ISecurityCenter {
    String encrypt(String content);
    String decrypt(String password);
}