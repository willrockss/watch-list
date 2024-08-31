INSERT INTO download_content_process(status, content_item_identity, torr_file_path, content_path)
VALUES
    ('INITIAL',    'mvi-001', '/fileA',   null),
    ('PROCESSING', 'mvi-002', '/fileB',   '/home/user/Videos/SomeClipB.mkv'),
    ('FINISHED',   'mvi-003', '/fileC',   '/home/user/Videos/SomeClipC.mkv'),
    ('ERROR',      'mvi-004', '/fileD',   null),
    ('INITIAL',    'mvi-005', '/file005', null); -- for update test