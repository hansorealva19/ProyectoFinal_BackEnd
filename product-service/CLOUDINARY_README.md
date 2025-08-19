Setup Cloudinary for product-service

1) Environment variables (or add to application.properties local):

CLOUDINARY:
- cloudinary.cloud-name=dqixr8zcs
- cloudinary.api-key=539793398796523
- cloudinary.api-secret=lKp05815lN7U2XtRWSvvnngGULs

2) Restart product-service after setting these.

3) Example curl to create a product with image (multipart):

curl -v -X POST \
  -F 'product={"name":"Test product","description":"desc","price":9.99,"stock":10,"category":"misc"};type=application/json' \
  -F 'image=@/path/to/image.jpg' \
  http://localhost:8083/api/products

Notes:
- The service uploads the image to Cloudinary and stores the secure_url in Product.imageUrl.
- If you prefer client-side (unsigned) uploads, tell me and I'll implement the frontend flow instead.
