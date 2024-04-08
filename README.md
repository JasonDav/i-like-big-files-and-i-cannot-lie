# Java Large File Streaming Investigations

## Running
1. Run `docker compose up --build` in the root directory.
2. Navigate to `localhost` and upload a file.
3. Files can be listed at `localhost:9000/files`.

Tests were run with a 10Gb file.

## Multipart
With multipart in Spring the file is loaded piece by piece into memory and then onto disk.
The File is then read piece by piece back into memory from storage and sent on.
This approach is blocking and any attempts to make the file stream directly out of the multipart of the request have failed.
At the moment it looks impossible with this version of Spring or I haven't found a working solution yet.

## Blob
This approach is the simplest and unsurprisingly works as expected.
The file is directly proxied to the destination server without blocking.

## WebSockets
Likely the same as the blob approach so is yet to be implemented.

# TODO
* [x]  Add Multipart solution.
* [x]  Add raw Octet Stream Solution.
* [ ]  Add WebSocket Solution.
* [ ]  Add some profiling to the applications or containers.