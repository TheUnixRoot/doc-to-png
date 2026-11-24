#!/usr/bin/env python3
import os
from PIL import Image, ImageDraw, ImageFont

res_dir = "/data/data/com.termux/files/home/raiz/doc-to-png/app/src/main/res"

def draw_doctopng_logo(draw, cx, cy, radius):
    # Sleek camera & typography symbol
    # Indigo & Cyan palette
    accent_color = (56, 189, 248, 255)  # Cyan #38BDF8
    primary_color = (99, 102, 241, 255) # Indigo #6366F1
    white_color = (255, 255, 255, 255)
    
    # Outer ring
    draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
                 outline=accent_color, width=max(2, int(radius * 0.12)))
                 
    # Inner camera aperture / viewfinder shape
    r_in = radius * 0.55
    draw.rectangle([cx - r_in, cy - r_in * 0.7, cx + r_in, cy + r_in * 0.7],
                   outline=primary_color, width=max(2, int(radius * 0.1)))
                   
    # Center lens circle
    r_center = radius * 0.3
    draw.ellipse([cx - r_center, cy - r_center, cx + r_center, cy + r_center],
                 fill=white_color)
                 
    # "T" typography indicator
    dot_r = radius * 0.1
    draw.ellipse([cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r],
                 fill=primary_color)

def generate_foreground(size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy = size // 2, size // 2
    radius = int(size * 0.34)
    draw_doctopng_logo(draw, cx, cy, radius)
    return img

def generate_full_icon(size):
    # Dark studio background
    img = Image.new("RGBA", (size, size), (15, 17, 23, 255)) # #0F1117
    draw = ImageDraw.Draw(img)
    
    margin = size * 0.05
    draw.rounded_rectangle([margin, margin, size - margin, size - margin],
                           radius=size * 0.22, fill=(24, 27, 36, 255),
                           outline=(46, 53, 73, 255), width=max(1, int(size * 0.03)))
    
    cx, cy = size // 2, size // 2
    radius = int(size * 0.35)
    draw_doctopng_logo(draw, cx, cy, radius)
    return img

def generate_round_icon(size):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    cx, cy = size // 2, size // 2
    r = size // 2 - 2
    draw.ellipse([cx - r, cy - r, cx + r, cy + r],
                 fill=(24, 27, 36, 255), outline=(56, 189, 248, 255),
                 width=max(1, int(size * 0.03)))
    
    radius = int(size * 0.35)
    draw_doctopng_logo(draw, cx, cy, radius)
    return img

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

for folder, sz in densities.items():
    dpath = os.path.join(res_dir, folder)
    os.makedirs(dpath, exist_ok=True)
    
    # Legacy launcher icons
    icon = generate_full_icon(sz)
    icon.save(os.path.join(dpath, "ic_launcher.png"), "PNG")
    
    round_icon = generate_round_icon(sz)
    round_icon.save(os.path.join(dpath, "ic_launcher_round.png"), "PNG")
    
    # Adaptive foreground
    fg = generate_foreground(sz)
    fg.save(os.path.join(dpath, "ic_launcher_foreground.png"), "PNG")

print("DocToPNG icons generated successfully.")
