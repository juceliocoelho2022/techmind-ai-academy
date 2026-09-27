ALTER TABLE platform_settings
    ADD COLUMN whatsapp_number VARCHAR(30);

UPDATE platform_settings
SET support_email = 'suporttecmind@gmail.com',
    whatsapp_number = '+5511911625945'
WHERE id = 1;
