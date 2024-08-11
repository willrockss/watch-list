INSERT INTO download_content_process (status, content_item_identity, torr_file_path)
VALUES
    ('INITIAL',    'mvi-001', '/fileA'),
    ('PROCESSING', 'mvi-002', '/fileB'),
    ('FINISHED',   'mvi-003', '/fileC'),
    ('ERROR', 'mvi-004', '/fileD');