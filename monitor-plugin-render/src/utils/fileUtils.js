/**
 * 显示类型转换，主要用于文件显示图标
 * @param fileName
 * @returns {*|string}
 */
export const convertFileType = function (extension) {
    // 获取文件扩展名
    extension = extension.toLowerCase();

    // 定义文件类型映射
    //folder/docx/xls/image/mp3/pdf/ppt/txt/video/zip/none
    const fileTypeMap = {
        'directory': 'folder',
        'jpg': 'image',
        'jpeg': 'image',
        'png': 'image',
        'gif': 'image',
        'bmp': 'image',
        'svg': 'image',
        'webp': 'image',
        'mp4': 'video',
        'avi': 'video',
        'mov': 'video',
        'wmv': 'video',
        'mp3': 'video',
        'wav': 'video',
        'flac': 'video',
        'm4a': 'video',
        'pdf': 'pdf',
        'doc': 'docx',
        'docx': 'docx',
        'xls': 'xls',
        'xlsx': 'xls',
        'ppt': 'ppt',
        'pptx': 'ppt',
        'txt': 'txt',
        'json': 'txt',
        'xml': 'txt',
        'html': 'txt',
        'css': 'txt',
        'js': 'txt',
        'md': 'txt',
        'zip': 'zip',
        'rar': 'zip',
        '7z': 'zip',
        'tar': 'zip',
        'gz': 'zip',
    };

    // 根据扩展名查找文件类型
    return fileTypeMap[extension] || 'none';
}

/**
 * 格式化文件显示格式
 * @param fileList
 * @returns {*[]}
 */
export const formatFileList = function (fileList) {
    var resArr = [];
    if (fileList == null) return resArr;
    fileList.forEach((value, key) => {
        let item = {
            key: '', type: '', name: '', data: {
                lastModified: '', fileType: '', size: '', status: '', version: '', releaseTime: '', fileName: '',
            }
        };
        item.key = key;
        item.name = value['name'];
        item.type = convertFileType(value['type']);
        item.data.size = value['size'] ? value['size'] + 'KB' : "";
        item.data.fileType = value['type'] === "Directory" ? "文件夹" : value['type'];
        item.data.lastModified = value['lastModified']
        item.data.path = value['path']
        item.data.status = value['version'] ? '已发布' : '未发布';
        item.data.version = value['version'] ? value['version'] : '';
        item.data.releaseTime = value['releaseTime'] ? value['releaseTime'] : '';
        item.data.isCurrentVersion = value['isCurrentVersion'] ? "当前版本" : "";
        item.data.fileName = value['name'];
        resArr.push(item)
    })
    return resArr;
}
