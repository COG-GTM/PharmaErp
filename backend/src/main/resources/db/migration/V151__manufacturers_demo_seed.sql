-- T06: demo manufacturers, suppliers and distributors (idempotent on the unique name).
INSERT IGNORE INTO `manufacturer`
    (`manufacturer_name`, `manufacturer_type`, `licence_number`, `contact_person`, `manufacturer_address`,
     `manufacturer_email`, `manufacturer_phone`, `city`, `district`, `state`, `gstin`)
VALUES
    ('Sun Pharmaceutical Industries Ltd', 'MANUFACTURER', 'MH-MFG-2019-000412', 'Rakesh Mehta',
     'Sun House, Western Express Highway, Goregaon (E)', 'supply@sunpharma.example', '+912243244324',
     'Mumbai', 'Mumbai Suburban', 'Maharashtra', '27AAACS3456K1Z5'),
    ('Cipla Ltd', 'MANUFACTURER', 'MH-MFG-2017-000188', 'Anita Desai',
     'Cipla House, Peninsula Business Park, Lower Parel', 'orders@cipla.example', '+912224826000',
     'Mumbai', 'Mumbai City', 'Maharashtra', '27AAACC1450B1ZB'),
    ('Zydus Lifesciences Ltd', 'MANUFACTURER', 'GJ-MFG-2018-000077', 'Nirav Patel',
     'Zydus Corporate Park, Khoraj', 'trade@zydus.example', '+917971800000',
     'Ahmedabad', 'Ahmedabad', 'Gujarat', '24AAACC6253G1ZT'),
    ('Gujarat Pharma Supplies Pvt Ltd', 'SUPPLIER', 'GJ-WS-20B-2021-1043', 'Hetal Shah',
     '14 Ashram Road, Navrangpura', 'sales@gujpharmasupplies.example', '+917926401122',
     'Ahmedabad', 'Ahmedabad', 'Gujarat', '24AABCG1234L1Z2'),
    ('MedLine Bulk Chemicals', 'SUPPLIER', 'MH-WS-20B-2020-2210', 'Sameer Kulkarni',
     'Plot 22, MIDC Bhosari', 'procurement@medline-bulk.example', '+912027120099',
     'Pune', 'Pune', 'Maharashtra', '27AAFCM7788Q1Z9'),
    ('Western India Distributors', 'DISTRIBUTOR', 'GJ-WS-21B-2022-0519', 'Kiran Joshi',
     '3rd Floor, Race Course Circle', 'dispatch@wid.example', '+912652331144',
     'Vadodara', 'Vadodara', 'Gujarat', '24AAGFW5566M1Z4'),
    ('Deccan Pharma Logistics', 'DISTRIBUTOR', 'KA-WS-21B-2023-0307', 'Lakshmi Rao',
     '88 Hosur Road, Electronic City', 'ops@deccanpharma.example', '+918041105566',
     'Bengaluru', 'Bengaluru Urban', 'Karnataka', '29AAJCD9900R1Z1');
