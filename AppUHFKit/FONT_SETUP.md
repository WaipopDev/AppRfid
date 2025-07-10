# การติดตั้งฟอนต์ SukhumvitSet

## ขั้นตอนการติดตั้ง

1. **คัดลอกไฟล์ฟอนต์**
   - คัดลอกไฟล์ `SukhumvitSet-Bold.ttf` ไปที่ `app/src/main/assets/fonts/`
   - คัดลอกไฟล์ `SukhumvitSet-Medium.ttf` ไปที่ `app/src/main/assets/fonts/`

2. **โครงสร้างโฟลเดอร์**
   ```
   app/src/main/assets/fonts/
   ├── SukhumvitSet-Bold.ttf
   └── SukhumvitSet-Medium.ttf
   ```

3. **การใช้งาน**
   - ฟอนต์จะถูกโหลดอัตโนมัติเมื่อแอปเริ่มต้น
   - SukhumvitSet-Bold ใช้สำหรับ header และปุ่ม
   - SukhumvitSet-Medium ใช้สำหรับ footer และข้อความในตาราง

## หมายเหตุ
- หากไฟล์ฟอนต์ไม่พบ แอปจะใช้ฟอนต์ระบบเป็นค่าเริ่มต้น
- ฟอนต์จะถูก cache ไว้เพื่อประสิทธิภาพที่ดีขึ้น 