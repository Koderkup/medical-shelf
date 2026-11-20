#!/usr/bin/env python3
import hmac
import hashlib
import sys

ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
SECRET_KEY = "MED_SHELF_PRO_SECRET_KEY_2026_OFFLINE_RUSTORE"

def generate_key(device_code: str) -> str:
    normalized = device_code.replace("-", "").strip().upper()
    if len(normalized) != 8:
        raise ValueError("Device Code must be 8 characters (format XXXX-XXXX or XXXXXXXX)")

    key_bytes = SECRET_KEY.encode('utf-8')
    msg_bytes = normalized.encode('utf-8')
    h = hmac.new(key_bytes, msg_bytes, hashlib.sha256).digest()

    chars = []
    alphabet_len = len(ALPHABET)
    for i in range(8):
        byte_val = h[i]
        chars.append(ALPHABET[byte_val % alphabet_len])

    code_str = "".join(chars)
    return f"MP-{code_str[:4]}-{code_str[4:]}"

def main():
    if len(sys.argv) > 1:
        dev_code = sys.argv[1]
    else:
        dev_code = input("Enter Device Code (e.g. 5A7K-9P2R): ").strip()

    try:
        lic_key = generate_key(dev_code)
        print("\n==========================================")
        print(f"Device Code: {dev_code.upper()}")
        print(f"License Key: {lic_key}")
        print("==========================================\n")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    main()
