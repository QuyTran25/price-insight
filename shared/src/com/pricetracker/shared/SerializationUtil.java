package com.pricetracker.shared;

import java.io.*;

/**
 * SerializationUtil - Tiện ích nhập xuất đối tượng
 * Hỗ trợ serialize/deserialize objects thành byte array hoặc file
 * Dùng chung cho Client và Server
 */
public class SerializationUtil {

    /**
     * Serialize một object thành mảng byte
     * @param obj Object cần serialize (phải implements Serializable)
     * @return Mảng byte chứa object đã serialize
     * @throws IOException Nếu có lỗi trong quá trình serialize
     */
    public static byte[] serialize(Object obj) throws IOException {
        if (obj == null) {
            throw new IllegalArgumentException("Object không được null");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            
            oos.writeObject(obj);
            oos.flush();
            return baos.toByteArray();
        }
    }

    /**
     * Deserialize mảng byte thành object
     * @param data Mảng byte chứa object đã serialize
     * @return Object đã được deserialize
     * @throws IOException Nếu có lỗi đọc dữ liệu
     * @throws ClassNotFoundException Nếu không tìm thấy class của object
     */
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Dữ liệu không được null hoặc rỗng");
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            
            return ois.readObject();
        }
    }

    /**
     * Deserialize với type safety
     * @param <T> Kiểu dữ liệu mong muốn
     * @param data Mảng byte chứa object đã serialize
     * @param type Class của object cần deserialize
     * @return Object đã được deserialize với đúng kiểu
     * @throws IOException Nếu có lỗi đọc dữ liệu
     * @throws ClassNotFoundException Nếu không tìm thấy class
     * @throws ClassCastException Nếu kiểu dữ liệu không khớp
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] data, Class<T> type) throws IOException, ClassNotFoundException {
        Object obj = deserialize(data);
        if (!type.isInstance(obj)) {
            throw new ClassCastException("Object không phải kiểu " + type.getName());
        }
        return (T) obj;
    }

    /**
     * Serialize object và ghi ra file
     * @param obj Object cần serialize
     * @param filePath Đường dẫn file đích
     * @throws IOException Nếu có lỗi ghi file
     */
    public static void serializeToFile(Object obj, String filePath) throws IOException {
        if (obj == null) {
            throw new IllegalArgumentException("Object không được null");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Đường dẫn file không hợp lệ");
        }

        try (FileOutputStream fos = new FileOutputStream(filePath);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            
            oos.writeObject(obj);
            oos.flush();
        }
    }

    /**
     * Đọc object từ file
     * @param filePath Đường dẫn file nguồn
     * @return Object đã được deserialize
     * @throws IOException Nếu có lỗi đọc file
     * @throws ClassNotFoundException Nếu không tìm thấy class
     */
    public static Object deserializeFromFile(String filePath) throws IOException, ClassNotFoundException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Đường dẫn file không hợp lệ");
        }

        try (FileInputStream fis = new FileInputStream(filePath);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            return ois.readObject();
        }
    }

    /**
     * Đọc object từ file với type safety
     * @param <T> Kiểu dữ liệu mong muốn
     * @param filePath Đường dẫn file nguồn
     * @param type Class của object cần deserialize
     * @return Object đã được deserialize với đúng kiểu
     * @throws IOException Nếu có lỗi đọc file
     * @throws ClassNotFoundException Nếu không tìm thấy class
     * @throws ClassCastException Nếu kiểu dữ liệu không khớp
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserializeFromFile(String filePath, Class<T> type) 
            throws IOException, ClassNotFoundException {
        Object obj = deserializeFromFile(filePath);
        if (!type.isInstance(obj)) {
            throw new ClassCastException("Object không phải kiểu " + type.getName());
        }
        return (T) obj;
    }

    /**
     * Clone một object bằng cách serialize rồi deserialize
     * @param <T> Kiểu dữ liệu
     * @param obj Object cần clone
     * @return Bản sao của object
     * @throws IOException Nếu có lỗi serialize
     * @throws ClassNotFoundException Nếu có lỗi deserialize
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deepClone(T obj) 
            throws IOException, ClassNotFoundException {
        if (obj == null) {
            return null;
        }
        
        byte[] data = serialize(obj);
        return (T) deserialize(data);
    }

    /**
     * Kiểm tra xem một object có thể serialize được không
     * @param obj Object cần kiểm tra
     * @return true nếu có thể serialize, false nếu không
     */
    public static boolean isSerializable(Object obj) {
        if (obj == null) {
            return false;
        }
        
        try {
            serialize(obj);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Tính kích thước của object sau khi serialize (bytes)
     * @param obj Object cần tính kích thước
     * @return Kích thước tính bằng bytes, hoặc -1 nếu không serialize được
     */
    public static long getSerializedSize(Object obj) {
        if (obj == null) {
            return 0;
        }
        
        try {
            byte[] data = serialize(obj);
            return data.length;
        } catch (IOException e) {
            return -1;
        }
    }
}
