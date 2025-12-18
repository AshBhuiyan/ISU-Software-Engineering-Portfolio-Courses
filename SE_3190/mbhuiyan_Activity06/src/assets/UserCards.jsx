import 'bootstrap/dist/css/bootstrap.css';
import { Container, Card } from 'react-bootstrap';

export function UserCard({ picture, name, amount, married, address }) {
  return (
    <Container className="p-4">
      <Card className="user-card shadow-sm" style={{ width: '15rem' }}>
        <Card.Img
          variant="top"
          src={picture}
          style={{ width: '100%', objectFit: 'cover' }}
        />
        <Card.Body>
          <Card.Title>{name}</Card.Title>
          <Card.Text>Salary $ {amount}</Card.Text>
          <Card.Text>{married ? 'Married' : 'Single'}</Card.Text>
          <ul className="address-line">
            <li>Street: {address.street}</li>
            <li>{address.city}</li>
            <li>{address.state}</li>
          </ul>
        </Card.Body>
      </Card>
    </Container>
  );
}