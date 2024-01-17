FROM python:3.2

WORKDIR /usr/src/app
RUN pip install --upgrade --force-reinstall \
                flake8==2.5.4

COPY . .

CMD [ "python" ]