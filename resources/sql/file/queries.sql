--name: select-user-files
--Select all files owned by user
SELECT id, name, path, mime_type, size, ctime, mtime, GROUP_CONCAT(ft.tag) AS tags FROM user_file AS f
       LEFT JOIN user_file_tag AS ft ON f.id = ft.file_id WHERE user_id = :user_id GROUP BY id

--name: replace-file-tag!
REPLACE INTO user_file_tag (file_id, tag)
       VALUES (:file_id, :tag)

--name: select-file-count-and-path
SELECT count(id) AS 'usage', path FROM user_file WHERE path = (SELECT path FROM user_file WHERE id = :id)

--name: delete-file!
DELETE FROM user_file WHERE id = :id

--name: delete-files-block-file!
DELETE FROM page_block_files_has_file WHERE file_id = :file_id

--name: delete-file-tags!
DELETE FROM user_file_tag WHERE file_id = :file_id

--name: insert-file<!
INSERT INTO user_file (user_id, name, path, mime_type, size, ctime, mtime) VALUES (:user_id, :name, :path, :mime_type, :size, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())