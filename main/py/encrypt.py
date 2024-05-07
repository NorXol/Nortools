import os
import argparse
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes
from tqdm import tqdm
import subprocess
import zipfile

def clear_terminal():
    # Clear the terminal
    subprocess.call('clear', shell=True)

def generate_keys():
    # Generate two AES keys
    key1 = get_random_bytes(32)
    key2 = get_random_bytes(32)
    
    # Combine the keys into one
    combined_key = key1 + key2
    
    return key1, key2, combined_key

def encrypt_file(file_path, combined_key):
    chunk_size = 64 * 1024  # 64 KB

    # Split the combined key into two AES keys
    key1 = combined_key[:32]
    key2 = combined_key[32:]

    # Generate IVs
    iv1 = get_random_bytes(16)
    iv2 = get_random_bytes(16)

    # Create AES objects
    aes1 = AES.new(key1, AES.MODE_CBC, iv1)
    aes2 = AES.new(key2, AES.MODE_CBC, iv2)

    # Encrypt the file
    with open(file_path, 'rb') as infile:
        file_size = os.path.getsize(file_path)
        with open(file_path + '.enc', 'wb') as outfile:
            outfile.write(iv1)
            outfile.write(iv2)

            with tqdm(total=file_size, unit='B', unit_scale=True, desc='Encrypting') as pbar:
                while True:
                    chunk = infile.read(chunk_size)
                    if len(chunk) == 0:
                        break
                    elif len(chunk) % 16 != 0:
                        chunk += b' ' * (16 - len(chunk) % 16)

                    encrypted_chunk = aes1.encrypt(aes2.encrypt(chunk))
                    outfile.write(encrypted_chunk)
                    pbar.update(len(chunk))

def encrypt_folder(folder_path, combined_key):
    output_folder = "output"
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    zip_path = os.path.join(output_folder, os.path.basename(folder_path) + '.zip')
    # Compress the folder
    with zipfile.ZipFile(zip_path, 'w') as zipf:
        for root, _, files in os.walk(folder_path):
            for file in files:
                zipf.write(os.path.join(root, file), os.path.relpath(os.path.join(root, file), folder_path))

    # Encrypt the zip file
    encrypt_file(zip_path, combined_key)

    # Remove the temporary zip file
    os.remove(zip_path)

def process_command(command):
    args = command.split()
    if args[0] == 'enc':
        if len(args) < 2:
            print("Usage: enc <file_path/folder_path> [mode]")
            return

        path = args[1]
        mode = args[2] if len(args) >= 3 else 'single'

        if mode == 'folder' and not os.path.isdir(path):
            print(f"Error: {path} is not a folder. Use 'single' mode for single files.")
            return
    
        if mode == 'single' and not os.path.isfile(path):
            print(f"Error: {path} is not a file. Use 'folder' mode for folders.")
            return

        if mode == 'folder':
            key1, key2, combined_key = generate_keys()
            encrypt_folder(path, combined_key)
        else:
            if not os.path.exists(path):
                print(f"Error: File {path} not found.")
                return

            key1, key2, combined_key = generate_keys()
            encrypt_file(path, combined_key)
            
        print("File(s) encrypted successfully.")
        print("Combined AES key:", combined_key.hex())
    else:
        print(f"Unknown command: {command}")

def main():
    clear_terminal()

    explanation = """
    Welcome to Dual AES 256 Encryption Tool!

    This tool encrypts files or folders using dual AES 256 encryption.
    It generates two random AES 256-bit keys, combines them, and encrypts the file/folder.

    Usage: enc <file_path/folder_path> [mode]

    Modes:
      - single: Encrypt a single file.
      - folder: Encrypt a folder.

    Example: enc secret_file.txt single
             enc confidential_folder folder

    After encryption, the encrypted file will be saved with the extension ".enc".
    The combined AES key will be displayed for your reference.

    To exit, type 'exit'.
    """
    print(explanation)

    while True:
        command = input(">>> ")
        if command.lower() == 'exit':
            break
        process_command(command)

if __name__ == "__main__":
    main()
