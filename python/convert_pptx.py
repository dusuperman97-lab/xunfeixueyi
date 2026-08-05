"""
Convert PPTX to high-quality slide images using LibreOffice.
Usage: python convert_pptx.py <input.pptx> <output_dir>
Output: output_dir/ slide-001.png, slide-002.png, ..., slides.json
"""
import subprocess, os, sys, json, glob, shutil


def find_libreoffice():
    """Find LibreOffice soffice executable on Windows."""
    candidates = [
        r'C:\Program Files\LibreOffice\program\soffice.exe',
        r'C:\Program Files (x86)\LibreOffice\program\soffice.exe',
        os.path.expandvars(r'%LOCALAPPDATA%\Programs\LibreOffice\program\soffice.exe'),
        r'C:\LibreOffice\program\soffice.exe',
    ]
    # Also search in common locations
    for root in [r'C:\Program Files', r'C:\Program Files (x86)',
                 os.path.expandvars(r'%LOCALAPPDATA%\Programs')]:
        try:
            for dirpath, _, filenames in os.walk(root):
                if 'soffice.exe' in filenames and 'LibreOffice' in dirpath:
                    candidates.append(os.path.join(dirpath, 'soffice.exe'))
        except PermissionError:
            continue

    for path in candidates:
        if os.path.exists(path):
            return path
    return None


def convert_pptx_to_images(pptx_path, output_dir, soffice_path=None):
    """Convert PPTX to PNG images using LibreOffice."""
    pptx_path = os.path.abspath(pptx_path)
    output_dir = os.path.abspath(output_dir)
    os.makedirs(output_dir, exist_ok=True)

    if soffice_path is None:
        soffice_path = find_libreoffice()

    if not soffice_path or not os.path.exists(soffice_path):
        raise RuntimeError(
            "LibreOffice not found. Install it from https://www.libreoffice.org/download/\n"
            "Or place soffice.exe in a standard location."
        )

    print(f"Using LibreOffice: {soffice_path}")
    print(f"Input:  {pptx_path}")
    print(f"Output: {output_dir}")

    # Convert PPTX to PDF first (for best quality), then PDF to PNG
    cmd = [
        soffice_path,
        '--headless',
        '--convert-to', 'pdf',
        '--outdir', output_dir,
        pptx_path,
    ]

    print(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    print(result.stdout)
    if result.returncode != 0:
        print("STDERR:", result.stderr)
        raise RuntimeError(f"LibreOffice PDF conversion failed (code {result.returncode})")

    # Find the generated PDF
    pdf_files = glob.glob(os.path.join(output_dir, '*.pdf'))
    if not pdf_files:
        raise RuntimeError("No PDF generated")

    pdf_path = pdf_files[0]
    print(f"PDF generated: {pdf_path}")

    # Convert PDF to images using pdf2image or Pillow
    # Try pdf2image first, fall back to another approach
    try:
        from pdf2image import convert_from_path
        images = convert_from_path(pdf_path, dpi=150)
        image_files = []
        for i, img in enumerate(images):
            out_path = os.path.join(output_dir, f'slide-{i+1:03d}.png')
            img.save(out_path, 'PNG')
            image_files.append(out_path)
            print(f"  Saved slide {i+1}/{len(images)}")
    except ImportError:
        print("pdf2image not available, trying alternative...")
        # Use LibreOffice to directly export to images
        # First remove the PDF
        os.remove(pdf_path)
        # Convert directly to PNG
        cmd2 = [
            soffice_path,
            '--headless',
            '--convert-to', 'png',
            '--outdir', output_dir,
            pptx_path,
        ]
        print(f"Running: {' '.join(cmd2)}")
        result2 = subprocess.run(cmd2, capture_output=True, text=True, timeout=300)
        print(result2.stdout)
        if result2.returncode != 0:
            print("STDERR:", result2.stderr)
            raise RuntimeError(f"LibreOffice PNG conversion failed (code {result2.returncode})")

        # LibreOffice PNG output uses original filename + page number
        base = os.path.splitext(os.path.basename(pptx_path))[0]
        png_pattern = os.path.join(output_dir, f'{base} *.png')
        png_files = sorted(glob.glob(png_pattern))
        if not png_files:
            # Try without space
            png_files = sorted(glob.glob(os.path.join(output_dir, f'{base}*.png')))

        # Rename to standard format
        image_files = []
        for i, src in enumerate(png_files):
            dst = os.path.join(output_dir, f'slide-{i+1:03d}.png')
            shutil.move(src, dst)
            image_files.append(dst)
            print(f"  Saved slide {i+1}/{len(png_files)}")

    # Remove PDF if it still exists
    pdf_files = glob.glob(os.path.join(output_dir, '*.pdf'))
    for f in pdf_files:
        os.remove(f)

    # Write metadata
    total = len(image_files)
    meta = {
        'total': total,
        'files': [os.path.basename(f) for f in image_files],
    }
    with open(os.path.join(output_dir, 'slides.json'), 'w', encoding='utf-8') as f:
        json.dump(meta, f, ensure_ascii=False)

    print(f"\nDone: {total} slides converted to {output_dir}")
    return image_files


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)

    pptx_file = sys.argv[1]
    out_dir = sys.argv[2]
    soffice = sys.argv[3] if len(sys.argv) > 3 else None

    convert_pptx_to_images(pptx_file, out_dir, soffice)
