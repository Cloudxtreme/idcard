BEGIN;

COMMENT ON COLUMN access_groups.is_special
  IS 'This column marks special group membership decisions, not special group access decisions.';

INSERT INTO access_groups (card_group_id, name, description, is_special)
VALUES
       (1, '(Only Student)', 'All regular Student cards', true),
       (2, '(Only Piscine)', 'All Piscine temporary cards.', true),
       (3, '(Only Bocal)', 'All Bocal staff cards', true),
       (4, '(Only Security)', 'All Security access cards', true),
       (5, '(Only Employee)', 'All employee access cards', true),

       (11, '(All Cards)', 'All devices that look like a Card42 card (have a metadata file).', true),
       (12, '(All NFC Devices)', 'Anything that activates the card reader.', true),
       (13, 'General Access', 'All valid Card42 cards after secondary checks.', true),
       (14, 'Restricted Access', 'Bocal, Security, and Employee cards', true),
       (15, 'Students', 'Student and Piscine cards', true),
       (16, 'Revoked Cards', 'Cards in the revocation list', true),

       (40, 'Master Key', 'Unlocks every door', false),
       (41, '(Is a revoked card)', 'Automatically denied on every door. (Do not add this to door configurations.)', false),
       (42, 'System Administrator', 'Can approve any pending database object change.', false);

COMMIT;