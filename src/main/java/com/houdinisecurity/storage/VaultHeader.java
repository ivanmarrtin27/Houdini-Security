package com.houdinisecurity.storage;

import com.houdinisecurity.crypto.KdfAlgorithm;
import com.houdinisecurity.crypto.KdfParameters;
import com.houdinisecurity.crypto.SecureMemory;
import com.houdinisecurity.crypto.VaultCrypto;
import com.houdinisecurity.domain.CorruptedVaultException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public final class VaultHeader {

    private final short version;
    private final KdfParameters kdfParameters;
    private final byte[] wrappedDek;

    public VaultHeader(short version, KdfParameters kdfParameters, byte[] wrappedDek) {
        this.version = version;
        this.kdfParameters = kdfParameters;
        this.wrappedDek = wrappedDek.clone();
    }

    public short version() {
        return version;
    }

    public KdfParameters kdfParameters() {
        return kdfParameters;
    }

    public byte[] wrappedDekCopy() {
        return wrappedDek.clone();
    }

    public byte[] serialize() {
        byte[] salt = kdfParameters.saltCopy();
        int size = 4 + 2 + 1 + 1 + 4 + 4 + 4 + 2 + salt.length + wrappedDek.length;
        ByteBuffer buf = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(VaultCrypto.MAGIC);
        buf.putShort(version);
        buf.put(kdfParameters.algorithm().id());
        buf.put((byte) 0);
        buf.putInt(kdfParameters.memoryKiB());
        buf.putInt(kdfParameters.iterations());
        buf.putInt(kdfParameters.parallelism());
        buf.putShort((short) salt.length);
        buf.put(salt);
        buf.put(wrappedDek);
        SecureMemory.wipe(salt);
        return buf.array();
    }

    public static VaultHeader parse(byte[] data) {
        if (data == null || data.length < 24) {
            throw new CorruptedVaultException("Header too short");
        }
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        byte[] magic = new byte[4];
        buf.get(magic);
        if (!Arrays.equals(magic, VaultCrypto.MAGIC)) {
            throw new CorruptedVaultException("Invalid vault magic");
        }
        short version = buf.getShort();
        if (version != VaultCrypto.FORMAT_VERSION) {
            throw new CorruptedVaultException("Unsupported vault version: " + version);
        }
        KdfAlgorithm algorithm = KdfAlgorithm.fromId(buf.get());
        buf.get(); // reserved
        int memoryKiB = buf.getInt();
        int iterations = buf.getInt();
        int parallelism = buf.getInt();
        int saltLen = Short.toUnsignedInt(buf.getShort());
        if (saltLen < KdfParameters.MIN_SALT_LENGTH || buf.remaining() < saltLen + 12 + 16) {
            throw new CorruptedVaultException("Invalid salt/wrapped DEK length");
        }
        byte[] salt = new byte[saltLen];
        buf.get(salt);
        byte[] wrapped = new byte[buf.remaining()];
        buf.get(wrapped);
        KdfParameters params = new KdfParameters(algorithm, memoryKiB, iterations, parallelism, salt);
        SecureMemory.wipe(salt);
        return new VaultHeader(version, params, wrapped);
    }

    public static VaultHeader read(Path headerPath) throws IOException {
        return parse(Files.readAllBytes(headerPath));
    }

    public void writeAtomic(Path headerPath) throws IOException {
        Path tmp = headerPath.resolveSibling(headerPath.getFileName().toString() + ".tmp");
        byte[] data = serialize();
        try {
            Files.write(tmp, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(tmp, headerPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, headerPath, StandardCopyOption.REPLACE_EXISTING);
            }
            FilePermissions.tryRestrict(headerPath);
        } finally {
            SecureMemory.wipe(data);
            Files.deleteIfExists(tmp);
        }
    }

    public void wipe() {
        SecureMemory.wipe(wrappedDek);
    }
}
