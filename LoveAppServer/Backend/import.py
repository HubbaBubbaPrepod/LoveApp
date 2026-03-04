import os

# Расширения файлов, которые нужно объединить
EXTENSIONS = ('.js')
# Имя выходного файла
OUTPUT_FILE = 'combined_for_ai.txt'
# Кодировка файлов (обычно UTF-8)
ENCODING = 'utf-8'

def collect_files(root_dir):
    """Рекурсивно собирает все файлы с нужными расширениями."""
    files_list = []
    for dirpath, _, filenames in os.walk(root_dir):
        for f in filenames:
            if f.lower().endswith(EXTENSIONS):
                full_path = os.path.join(dirpath, f)
                files_list.append(full_path)
    return files_list

def merge_files(file_paths, output_path):
    """Записывает содержимое всех файлов в один, добавляя заголовки."""
    with open(output_path, 'w', encoding=ENCODING) as outfile:
        for file_path in sorted(file_paths):
            # Заголовок с относительным путём
            outfile.write(f"\n\n--- {os.path.relpath(file_path, start=os.getcwd())} ---\n\n")
            try:
                with open(file_path, 'r', encoding=ENCODING) as infile:
                    outfile.write(infile.read())
            except Exception as e:
                outfile.write(f"!!! Ошибка чтения файла: {e} !!!")
    print(f"✅ Готово! Объединённый файл сохранён как {output_path}")

if __name__ == '__main__':
    # Текущая папка (где лежит скрипт)
    current_dir = os.path.dirname(os.path.abspath(__file__))
    files = collect_files(current_dir)
    if files:
        merge_files(files, os.path.join(current_dir, OUTPUT_FILE))
        print(f"📄 Найдено файлов: {len(files)}")
    else:
        print("❌ Не найдено файлов с расширениями .kt, .ktx, .xml")