(() => {
  const DATABASE = 'veshinantam-local-scans';
  const STORE = 'pdfs';
  const metadataKey = key => `veshinantam.local-scan.${key}`;
  let databasePromise;

  const database = () => databasePromise ||= new Promise((resolve, reject) => {
    const request = indexedDB.open(DATABASE, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error('Local PDF storage is unavailable.'));
  });

  const save = async (key, file) => {
    const db = await database();
    await new Promise((resolve, reject) => {
      const transaction = db.transaction(STORE, 'readwrite');
      transaction.objectStore(STORE).put(file, key);
      transaction.oncomplete = resolve;
      transaction.onerror = () => reject(transaction.error || new Error('Could not save the PDF.'));
      transaction.onabort = () => reject(transaction.error || new Error('Could not save the PDF.'));
    });
  };

  const read = async key => {
    const db = await database();
    return new Promise((resolve, reject) => {
      const request = db.transaction(STORE, 'readonly').objectStore(STORE).get(key);
      request.onsuccess = () => resolve(request.result || null);
      request.onerror = () => reject(request.error || new Error('Could not read the PDF.'));
    });
  };

  window.veshinantamLocalScans = {
    readMetadata(key) {
      return localStorage.getItem(metadataKey(key));
    },
    choose(key, ordinal) {
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'application/pdf,.pdf';
      input.onchange = async () => {
        const file = input.files?.[0];
        if (!file) return;
        if (file.size < 8 || file.size > 250 * 1024 * 1024 ||
            !(file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf'))) {
          alert('Choose a readable PDF smaller than 250 MB.');
          return;
        }
        try {
          const signature = new TextDecoder().decode(await file.slice(0, 5).arrayBuffer());
          if (signature !== '%PDF-') throw new Error('The selected file is not a PDF.');
          await save(key, file);
          localStorage.setItem(metadataKey(key), JSON.stringify({
            fileName: file.name, anchorOrdinal: ordinal, anchorPdfPage: 0, calibrated: false,
          }));
        } catch (error) {
          alert(error?.message || 'Could not save the PDF on this device.');
        }
      };
      input.click();
    },
    align(key, ordinal, page) {
      if (!Number.isInteger(page) || page < 0) return;
      const raw = localStorage.getItem(metadataKey(key));
      if (!raw) return;
      const metadata = JSON.parse(raw);
      localStorage.setItem(metadataKey(key), JSON.stringify({
        ...metadata, anchorOrdinal: ordinal, anchorPdfPage: page, calibrated: true,
      }));
    },
    async open(key, page) {
      const viewer = window.open('', '_blank');
      try {
        const file = await read(key);
        if (!file) throw new Error('The saved PDF is no longer available. Choose it again.');
        const url = URL.createObjectURL(file);
        const destination = `${url}#page=${Math.max(1, Math.floor(page) + 1)}`;
        if (viewer) viewer.location.replace(destination);
        else location.assign(destination);
      } catch (error) {
        viewer?.close();
        alert(error?.message || 'Could not open the PDF.');
      }
    },
  };
})();
